# CÓMO FUNCIONA

Backend de un marketplace agropecuario. Los productores publican lo que venden —semillas, maquinaria, insumos—, los compradores lo cargan al carrito y cierran la compra. Las fotos que se suben pasan por un verificador con IA que chequea que la imagen se corresponda con la categoría declarada.

TPO de Aplicaciones Interactivas · Spring Boot 4.1 · Java 25 · MySQL 8

> **Documento completo:** [`COMO-FUNCIONA.html`](COMO-FUNCIONA.html) — diagramas de las tres capas, el modelo de datos, el flujo de compra y la máquina de estados. GitHub no renderiza HTML acá adentro: descargalo y abrilo en el navegador, o mirá el resumen de abajo.

---

## Levantarlo

```bash
./mvnw spring-boot:run
```

Queda en `http://localhost:4002`. Necesita MySQL con una base `marketplace` creada; las tablas las genera Hibernate solo (`ddl-auto=update`). Las credenciales están en `src/main/resources/application.properties`.

Para probarlo hay una colección de Insomnia lista en [`marketplace.insomnia.json`](marketplace.insomnia.json): 39 requests divididos en **CLIENTE** y **ADMIN**, cada uno con su descripción.

---

## Las tres capas

```
Insomnia  ──JSON──▶  controllers  ──Request──▶  service  ──entidad──▶  repository  ──▶  MySQL
          ◀──JSON──               ◀─Response──           ◀─entidad──
```

Lo que hace que la separación sea real y no sólo tres carpetas es **qué objeto viaja por cada tramo**. Una entidad JPA nunca sale de la capa de servicios: entra un `Request`, se traduce a entidad para tocar la base, y vuelve un `Response`. Por eso `GET /usuarios` no puede filtrar la contraseña por accidente — `UsuarioResponse` directamente no tiene ese campo.

Ninguna clase hace `new` de otra: todas declaran sus dependencias como campos `final` y Spring las inyecta por constructor. No hay un solo `@Autowired` sobre un campo en todo el proyecto.

### Mapa del código

Los controllers están agrupados por dominio, siguiendo el ejemplo de la cátedra: `controllers/productos`, `controllers/ordenes`, etc., cada uno con su controller y sus DTOs. `controllers/config` guarda las cuatro clases de Spring Security, y `controllers/common` lo que no tiene dueño.

| Paquete | Clases | Responsabilidad |
|---|---|---|
| `controllers/*` | 34 | 8 controllers, sus 22 DTOs y las 4 clases de seguridad, agrupados por dominio. |
| `service` | 17 | Las reglas: validaciones, cálculos, transacciones. 7 interfaces + 7 impl + 3 sin interfaz. |
| `repository` | 8 | Interfaces de Spring Data. No hay una línea de SQL en el proyecto. |
| `entity` | 14 | 10 entidades JPA y 4 enums, guardados como texto. |

Cada orden guarda **cuándo** se creó y cuándo fue su último cambio de estado. Con eso se puede medir el tiempo de despacho de un vendedor o cuánto lleva una orden trabada, que es la base de cualquier métrica de reputación. Lo que no se puede reconstruir es el camino completo —cuándo pasó a PAGADA y cuándo se canceló, por separado—: para eso haría falta una tabla de historial.
| `exceptions` | 27 | Una por regla de negocio, cada una con su código HTTP en `@ResponseStatus`. |

Adentro de `controllers` hay una carpeta por dominio, y cada una lleva su controller y sus DTOs:

```
controllers/
  auth/       AutenticacionController · LoginRequest · TokenResponse
  carritos/   CarritosController · CarritoResponse · ItemCarritoRequest · ItemCarritoResponse
  categorias/ · fotos/ · ordenes/ · productos/ · usuarios/ · wishlist/
  config/     SecurityConfig · ApplicationConfig · JwtAuthenticationFilter · JwtService
  common/     MensajeResponse, que usan cuatro controllers y no tiene dueño
```

---

## Los flujos que importan

### Publicar un producto

Un `POST /productos` **no publica nada**: deja el producto en `BORRADOR` y devuelve el aviso de que falta la foto. Se publica solo cuando entra la primera imagen. Así el catálogo nunca tiene publicaciones sin foto.

```
BORRADOR ──sube la primera foto──▶ PUBLICADO ──▶ PAUSADO
   ▲                                             │
   └──────── borra la última foto ◀──────────────┘
```

Pausar, dar de baja o quedarse sin fotos **saca el producto de todos los carritos** donde estuviera cargado. Sin eso el comprador se enteraría recién al pagar.

### Carrito y wishlist

Se parecen —uno por usuario, con ítems y vencimiento— pero resuelven cosas distintas, y por eso son entidades separadas:

| | Carrito | Wishlist |
|---|---|---|
| Qué es | una compra a punto de cerrarse | una lista de intenciones |
| Cantidad por ítem | sí | no: querer algo dos veces no significa nada |
| Totales | subtotal y total | ninguno |
| Si el producto se pausa | **se va** del carrito | **se queda**, marcado `disponible: false` |
| Agregar dos veces | acumula cantidad | no hace nada |
| Vence a los | 30 días | 8 meses |

Que el ítem se quede cuando el producto sale de circulación es el punto de la wishlist: sirve para volver a mirarlo cuando vuelva. En el carrito sería lo contrario — descubrir al pagar que lo que ibas a comprar ya no está.

Ninguna de las dos tiene tarea programada: la limpieza es perezosa, ocurre cuando alguien mira la lista después de vencida. Y una lista vacía no vence, porque no hay nada que limpiar.

### Comprar

Nadie puede comprar lo que él mismo publica: la regla se chequea al agregar al carrito y otra vez al cerrar la orden, porque los chequeos de la orden no alcanzan — preguntan si sos el comprador y si sos el vendedor, y cuando sos los dos ambas dan verdadero.

`POST /ordenes` cierra el carrito. Si mezcla productos de varios vendedores genera **una orden por vendedor**, porque una orden es una transacción entre dos personas: con una sola no se podría representar que un vendedor ya despachó y el otro no. Valida el stock de todo antes de escribir nada, así un ítem sin stock no deja órdenes a medio crear.

La orden representa **la plata, no la logística**. Por eso tiene sólo los estados del cobro: que la mercadería se despachó, está en tránsito o llegó son hechos del envío, que tiene otro responsable y se modela aparte.

```
PENDIENTE ──▶ PAGADA
    └──────┴───▶ CANCELADA
```

**PAGADA** la pide el comprador; **CANCELADA**, cualquiera de los dos. `PAGADA` y `CANCELADA` son finales: de ahí no se sale. Si la transición existe pero le toca a la otra parte, 403. Si no existe desde el estado actual, 409.

### Subir una foto

La imagen se guarda como `byte[]` en la misma tabla. Antes de guardarse se le pregunta a Gemini si se corresponde con la categoría del producto. El prompt **se arma leyendo las categorías de la base**, no de una lista fija: cuando el admin crea una categoría nueva, la siguiente verificación ya la contempla.

| Confianza | Resultado |
|---|---|
| ≥ 0,7 | `APROBADA` |
| entre 0,4 y 0,7 | `EN_REVISION` — la resuelve un admin a mano |
| ≤ 0,4 | `RECHAZADA` — la subida falla |

La clave de Gemini está escrita en `application.properties`; la variable de entorno `GEMINI_API_KEY`, si existe, le gana. Si la API no está disponible o la clave no sirve, la foto queda en `EN_REVISION`: la verificación no puede ser un punto único de falla para publicar.

---

## Autenticación

Nada de lo privado se puede pedir sin un token. Se consigue en uno de dos lugares:

```bash
POST /auth/registro   # crea la cuenta y ya devuelve el token
POST /auth/login      # mail y contraseña a cambio del token
```

Los dos responden lo mismo:

```json
{"access_token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhbmFAdGVzdC5jb20i..."}
```

y a partir de ahí todo va con el header:

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Qué es ese texto.** Tres partes separadas por puntos: `header.payload.firma`. Las dos primeras son JSON en Base64 y **se leen sin ninguna clave** — pegá un token en jwt.io y vas a ver el mail y el vencimiento en claro. La tercera es `HMAC-SHA512(header+payload, clave)`. O sea que un JWT **no oculta, garantiza que no lo tocaron**: cualquiera puede leerlo, pero sólo el servidor puede fabricar uno. Por eso adentro va el mail y la fecha, nunca la contraseña.

**Qué pasa en cada request.** `JwtAuthenticationFilter` corre antes que cualquier controller: lee el header, verifica la firma, busca el usuario y lo deja en el `SecurityContext`. Si no hay token, o venció, o la firma no cierra, no rechaza nada — deja el contexto vacío y sigue. Quién decide si eso alcanza es `SecurityConfig`, y esa división es lo que permite que convivan rutas públicas y privadas sin un solo `if`.

**Lo público** es el catálogo: `GET /productos`, `/categorias`, `/fotos`, más el alta de usuario y las dos rutas de auth. Todo lo demás pide token.

**El token dura 24 horas** y no se guarda en ningún lado: que sea válido se decide verificando la firma y la fecha, no buscándolo en una tabla. Esa es la razón por la que no se puede invalidar uno antes de que venza.

**Después de loguearte**, `GET /usuarios/me` te dice quién sos. El login devuelve sólo el token, así que ese es el endpoint que usa el frontend para saber a quién saludar y si mostrar el panel de administración.

Las contraseñas se guardan con **BCrypt**, que incluye una sal distinta en cada hash y es lento a propósito. Nunca se desencripta: para verificar un login se hashea lo que llega y se comparan los hashes.

---

## Permisos

El id de quien pide la operación sale del **token**, no de la URL. Los controllers lo reciben con `@AuthenticationPrincipal` y se lo pasan a los services, que no se enteraron del cambio: siguen recibiendo un `Long` y decidiendo con las mismas reglas que cuando el id venía por query param.

Hay dos reglas: la **pertenencia** pregunta si el recurso es tuyo, y el **rol** pregunta si sos ADMIN. Las dos viven en `AutorizacionService`. El ADMIN **atraviesa la pertenencia**: puede operar sobre lo de cualquiera, porque modera todo el sistema. Esa excepción está dentro de `validarDuenio` y no repartida por los services, justamente para que valga en todos lados por igual y no se le escape ninguna operación.

| Operación | Quién |
|---|---|
| Editar, pausar o dar de baja un producto | su vendedor · ADMIN |
| Subir una foto | sólo el vendedor del producto |
| Borrar una foto | el vendedor del producto · ADMIN |
| Ver o tocar el carrito | sólo su dueño: la ruta no admite un id ajeno |
| Ver o tocar la wishlist | sólo su dueño |
| Editar o dar de baja una cuenta | esa misma cuenta · ADMIN |
| Ver una orden | comprador · vendedor · ADMIN |
| Listar órdenes | las propias — el ADMIN ve todas |
| Pagar una orden | sólo el comprador · ADMIN |
| Cancelar una orden | comprador · vendedor · ADMIN |
| Reactivar una cuenta o cambiar un rol | sólo ADMIN |
| Crear, editar o borrar categorías | ADMIN |
| Moderar fotos | ADMIN |
| Comprar un producto | cualquier CLIENTE menos su vendedor |
| Publicar, carritear o comprar | **el ADMIN no**: modera, no comercia |
| Ver el catálogo y crear una cuenta | abierto |

El ADMIN **no participa del marketplace**: no publica, no carga el carrito y no compra. No es que le falten permisos, es que le sobran — un admin que vende puede aprobarse sus propias fotos y despacharse sus propias órdenes, y uno que compra audita transacciones en las que es parte. Separar los dos papeles evita tener que confiar en que no los mezcle.

Tampoco puede saltear las reglas que no son de permisos: una transición de estado que no existe le da 409 igual, porque ahí el problema no es quién lo pide sino que la orden quedaría en un estado sin sentido. Ni quitarse el rol a sí mismo: si el último administrador se degrada, no queda nadie que pueda promover a nadie.

---

## Los 46 endpoints

Todo lo que no diga **público** necesita `Authorization: Bearer <token>`.

| | Ruta | Qué hace |
|---|---|---|
| `POST` | `/auth/registro` | Crea la cuenta y devuelve el token · **público** |
| `POST` | `/auth/login` | Mail y contraseña a cambio del token · **público** |
| `GET` | `/usuarios/me` | Quién soy, según el token |
| `GET` | `/categorias` | Listar. Con `?soloRaices=true`, sólo las de primer nivel · **público** |
| `GET` | `/categorias/{id}` | Una categoría |
| `GET` | `/categorias/{id}/subcategorias` | Sus hijas directas |
| `POST` | `/categorias` | Alta · **ADMIN** |
| `PUT` | `/categorias/{id}` | Editar o mover en el árbol · **ADMIN** |
| `DELETE` | `/categorias/{id}` | Baja. 409 si tiene hijas o productos · **ADMIN** |
| `GET` | `/productos` | Catálogo: sólo activos y PUBLICADOS, con filtros y orden por precio · **público** |
| `GET` | `/productos/mis-publicaciones` | Las propias, borradores y pausadas incluidas |
| `GET` | `/productos/todos` | El catálogo entero, sin los filtros del comprador · **ADMIN** |
| `GET` | `/productos/{id}` | Un producto, con categoría, vendedor y fotos |
| `POST` | `/productos` | Alta. Nace en BORRADOR |
| `PUT` | `/productos/{id}` | Editar |
| `PUT` | `/productos/{id}/estado` | Pausar o reanudar |
| `PUT` | `/productos/{id}/reactivar` | Devuelve al catálogo un producto dado de baja |
| `DELETE` | `/productos/{id}` | Baja lógica |
| `GET` | `/usuarios` | Listar los activos, sin contraseña |
| `GET` | `/usuarios/{id}` | Un usuario |
| `POST` | `/usuarios` | Alta sin token de vuelta; para eso está `/auth/registro` · **público** |
| `PUT` | `/usuarios/{id}` | Editar. El rol no se toca desde el body |
| `PUT` | `/usuarios/{id}/reactivar` | Vuelve a poner en circulación una cuenta · **ADMIN** |
| `PUT` | `/usuarios/{id}/rol` | Promueve o degrada · **ADMIN** |
| `DELETE` | `/usuarios/{id}` | Baja lógica, propia o por un **ADMIN**. El historial de órdenes sobrevive |
| `GET` | `/carrito` | Mi carrito, vaciado si venció |
| `POST` | `/carrito/items` | Agregar. Acumula si ya estaba |
| `PUT` | `/carrito/items/{item}` | Cambiar cantidad |
| `DELETE` | `/carrito/items/{item}` | Sacar un ítem |
| `DELETE` | `/carrito` | Vaciar |
| `GET` | `/wishlist` | Lo guardado para más adelante |
| `POST` | `/wishlist/items` | Guardar un producto. Idempotente |
| `DELETE` | `/wishlist/items/{item}` | Sacar uno |
| `DELETE` | `/wishlist` | Vaciar |
| `GET` | `/ordenes` | Sólo las del solicitante. Con `?rol=` se mira una punta; el ADMIN las ve todas |
| `GET` | `/ordenes/{id}` | Una orden con sus renglones. 403 si no sos parte |
| `POST` | `/ordenes` | Cerrar el carrito. Devuelve una orden por vendedor |
| `PUT` | `/ordenes/{id}/estado` | Avanzar el estado, según quién lo pida; el **ADMIN** puede cualquiera |
| `GET` | `/fotos?idProducto=` | Fotos de un producto |
| `GET` | `/fotos/{id}` | Metadatos de una foto |
| `POST` | `/fotos` | Subir. Verifica con IA antes de guardar |
| `GET` | `/fotos/{id}/contenido` | La imagen cruda, servible en un `<img>` |
| `GET` | `/fotos/{id}/base64` | La misma imagen dentro de un JSON |
| `GET` | `/fotos/pendientes` | Cola de revisión; con `?estado=` mira lo ya resuelto · **ADMIN** |
| `PUT` | `/fotos/{id}/revision` | Aprobar o rechazar · **ADMIN** |
| `DELETE` | `/fotos/{id}` | Borrado real. Si era la última, el producto vuelve a BORRADOR |

---

## Decisiones

**Baja lógica en usuarios y productos.** Las órdenes los referencian para siempre. Un `DELETE` real borraría el historial de ventas de un vendedor que no tuvo nada que ver. Se marcan `activo = false`: salen de los listados y no se pueden comprar, pero la orden se sigue leyendo.

**El binario en la base, no en disco.** La foto va como `byte[]` a una columna `LONGBLOB`. Evita coordinar el sistema de archivos con la base al borrar. El costo es que `Producto` carga sus fotos en `EAGER`.

**DTOs en las dos direcciones.** Los `Request` no tienen `id`, así que nadie puede pisar el de otro registro. Los `Response` no tienen `contrasena`, así que no puede filtrarse en un listado anidado.

**Excepciones con `@ResponseStatus`.** 27 excepciones de dominio, cada una con su código. Spring las traduce sola, sin un handler.

**Filtros con streams.** El catálogo se filtra en Java sobre `findAll()`. Es legible y alcanza para el volumen del TPO, pero trae toda la tabla a memoria.

---

## Lo que queda pendiente

- **Manejo centralizado de errores.** Sin `@RestControllerAdvice`, un error no contemplado sale como 500 con el SQL adentro.
- **Paginación.** Todos los listados devuelven la colección completa.
- **Bloqueo pesimista en el stock.** Dos compras simultáneas del último producto pueden dejar stock negativo.
- **Rango del descuento.** No valida estar entre 0 y 100: un valor mayor da totales negativos.
- **WebP.** `ImageIO` no lo lee, así que esas subidas saltean la verificación con IA.
