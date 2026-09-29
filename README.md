# CÓMO FUNCIONA

Backend de un marketplace agropecuario. Los productores publican lo que venden —semillas, maquinaria, insumos—, los compradores lo cargan al carrito y cierran la compra. Las fotos que se suben pasan por un verificador con IA que chequea que la imagen se corresponda con la categoría declarada. Después de la compra hay una logística real: el vendedor despacha, una empresa de transporte mueve el paquete y el comprador califica lo que recibió.

TPO de Aplicaciones Interactivas · Spring Boot 4.1 · Java 25 · MySQL 8

---

## Levantarlo

```bash
./mvnw spring-boot:run
```

Queda en `http://localhost:4002`. Necesita MySQL con una base `marketplace` creada; las tablas las genera Hibernate solo (`ddl-auto=update`). Las credenciales están en `src/main/resources/application.properties`.

> **Si ya tenías la base de antes**, mirá [Migraciones](#migraciones) al final. `ddl-auto=update` crea tablas y columnas nuevas, pero nunca modifica ni borra una que ya existe. Si la dropeás y la dejás armar de cero, no necesitás correr nada.

Para probarlo hay una colección de Insomnia lista en [`marketplace.insomnia.json`](marketplace.insomnia.json): 74 requests agrupados por dominio, cada uno con una descripción que explica no sólo qué hace sino por qué está así.

---

## Las tres capas

```
Insomnia  ──JSON──▶  controllers  ──Request──▶  service  ──entidad──▶  repository  ──▶  MySQL
          ◀──JSON──               ◀─Response──           ◀─entidad──
```

Lo que hace que la separación sea real y no sólo tres carpetas es **qué objeto viaja por cada tramo**. Una entidad JPA nunca sale de la capa de servicios: entra un `Request`, se traduce a entidad para tocar la base, y vuelve un `Response`. Por eso `GET /usuarios` no puede filtrar la contraseña por accidente — `UsuarioResponse` directamente no tiene ese campo.

Todos los services son **interfaz + implementación**: `ProductoService` es el contrato, `ProductoServiceImpl` el cómo. Son 20 pares. Lo que se inyecta en todos lados es la interfaz, así que cambiar una implementación no obliga a tocar a quien la usa. Los Pre y Post viven en la implementación; las interfaces van sin comentarios, porque la firma ya dice qué hace.

Ninguna clase hace `new` de otra: todas declaran sus dependencias como campos `final` y Spring las inyecta por constructor. No hay un solo `@Autowired` sobre un campo en el proyecto.

### Dos DTO para una persona

El mismo criterio de "que no exista lo que no tiene que salir" se aplica dos veces con los datos personales:

| | Qué lleva | Dónde |
|---|---|---|
| `UsuarioResponse` | nombre, apellido, usuario, **id, mail, dirección, rol, activo** | `/usuarios/me` y los endpoints de ADMIN |
| `UsuarioPublicoResponse` | nombre, apellido, usuario | anidado en productos, órdenes, carrito, wishlist, envíos |

La diferencia importa porque el catálogo es público. Mientras el vendedor viajaba como `UsuarioResponse` dentro de cada producto, **cualquier visitante sin cuenta podía recorrer `GET /productos` y quedarse con el mail y el domicilio de todos los que venden** — cerrar `GET /usuarios/{id}` no servía de nada mientras la misma información saliera por la puerta de al lado.

Hay un tercer DTO para lo mismo: `ProductoResumenResponse`. El catálogo no devuelve el producto entero sino lo que entra en una tarjeta —foto, precio, dónde está, si es nuevo o usado—, y el resto está en `GET /productos/{id}`. Bajó de 639 a 179 bytes por producto, y lo usan también el carrito, la wishlist y las órdenes.

### Mapa del código

| Paquete | Clases | Responsabilidad |
|---|---|---|
| `controllers/*` | 56 | 14 controllers, sus DTOs y las 4 clases de seguridad, agrupados por dominio |
| `service` | 40 | Las reglas: validaciones, cálculos, transacciones. 20 interfaces + 20 impl |
| `repository` | 14 | Interfaces de Spring Data. No hay una línea de SQL en el proyecto |
| `entity` | 27 | 16 entidades JPA y 11 enums, guardados como texto |
| `exceptions` | 43 | Una por regla de negocio, más el manejador y la clase base |

```
controllers/
  auth/  carritos/  categorias/  dashboard/  destacados/  envios/  fotos/
  notificaciones/  ofertas/  ordenes/  productos/  resenas/  usuarios/  wishlist/
  config/     SecurityConfig · ApplicationConfig · JwtAuthenticationFilter · JwtService
  common/     MensajeResponse y PaginaResponse, que no tienen dueño
```

---

## Errores: un solo lugar

Las 40 excepciones de negocio heredan de `ExcepcionDeNegocio`, que lleva su propio `HttpStatus` adentro:

```java
public abstract class ExcepcionDeNegocio extends Exception {
    private final HttpStatus estado;
}
```

Y `ManejadorDeErrores`, que es un `@RestControllerAdvice`, las traduce todas con un solo método:

```java
@ExceptionHandler(ExcepcionDeNegocio.class)
public ResponseEntity<Object> negocio(ExcepcionDeNegocio ex) {
    return ResponseEntity.status(ex.getEstado()).body(base(ex.getEstado(), ex.getMessage()));
}
```

El beneficio no es escribir menos: es que **hay una sola forma de error en toda la API**. Antes convivían tres —el 500 crudo de Spring con el stacktrace adentro, el `@ResponseStatus` sin cuerpo y los mensajes armados a mano—, y el front tenía que adivinar cuál le tocaba. Ahora todo sale igual:

```json
{"timestamp": "2026-09-29T14:02:11Z", "status": 409, "error": "Conflict",
 "message": "No hay stock suficiente de \"Tractor JD 5090E\""}
```

Agregar una regla nueva es crear una clase de tres líneas. Nadie toca el manejador.

---

## Los flujos que importan

### Publicar un producto

Un producto nace en **BORRADOR** y no aparece en el catálogo hasta tener **una foto aprobada**. Al crearlo hay que declarar provincia, si es nuevo o usado, y el año.

```
POST /productos          → BORRADOR
POST /fotos              → la IA la mira
                           APROBADA    → el producto pasa a PUBLICADO
                           EN_REVISION → sigue en BORRADOR
                           rechazada   → 422 y no se guarda nada
```

`PAUSADO` lo pone el vendedor cuando no quiere vender por un rato; vuelve a `PUBLICADO` cuando quiere.

### Subir una foto

El archivo entra por `multipart/form-data`, se re-codifica a JPEG con `ImageIO` y se manda a **Gemini** junto con la categoría declarada. La IA responde si la imagen coincide, con cuánta confianza, qué ve y qué categoría sugeriría.

- **coincide y confianza alta** → `APROBADA`, y el producto se publica.
- **no coincide con confianza alta** → se rechaza con **422** y un mensaje que explica qué vio: *"parece la foto de una pantalla de computadora, no de un tractor"*. No se guarda nada.
- **duda, o la IA no contestó** → `EN_REVISION`, y queda para que la mire un ADMIN.

**Una foto que no está APROBADA no se muestra en público.** Ni en la tarjeta del catálogo, ni en el detalle del producto, ni por `/fotos/{id}/contenido`. Para el que no es su vendedor ni ADMIN da **404 y no 403**: un 403 ya estaría contando que hay algo ahí, y alcanzaría con probar ids. Su dueño sí las ve, que es como se entera de que quedó algo por revisar.

### Comprar

```
POST /carrito/items          agregar
POST /ordenes                cerrar la compra
PUT  /ordenes/{id}/estado    PAGADA, sólo ADMIN
```

Al cerrar, **el carrito se parte por vendedor y por método de entrega**. Si un vendedor te vende un tractor (que no se despacha) y unas semillas, salen dos órdenes suyas: una a coordinar y otra por despacho. Cada renglón copia el nombre y el precio del momento, así que editar el producto después no reescribe la historia.

La **dirección de entrega se manda al comprar**, no sale del perfil: así podés mandarle algo a otra persona o a otra dirección. Se exige sólo si algo se va a despachar; una compra toda a coordinar no la necesita.

**PAGADA la marca el ADMIN.** No hay pasarela, así que ninguna de las dos partes puede probar que el dinero se movió: el vendedor diría que no le llegó y el comprador que ya pagó. Un tercero que no gana con ninguna de las dos respuestas es la única forma honesta de resolverlo sin integrar Mercado Pago. Una orden PAGADA ya no se cancela — ahí hay plata de por medio y habría que devolverla, que es un flujo que no existe.

### El envío

Cuando la orden pasa a PAGADA nace un `Envio`. La orden es la **plata**; el envío es la **logística**.

```
vendedor      PUT /envios/{id}/estado?estado=DESPACHADO
              → el sistema le da AGRO-CVSP2DBYWU, lo pega en el bulto
                y lo deja en la sucursal

despachante   POST /envios/recibir?numero=AGRO-CVSP2DBYWU
              → se lo asigna, pasa a EN_TRANSITO
              → RECIÉN AHÍ aparece la dirección de entrega

despachante   PUT /envios/{id}/estado?estado=ENTREGADO
```

**El número de seguimiento es la llave.** No hay una cola de envíos para mirar: en una sucursal uno carga el paquete que tiene en la mano, no uno de una lista. Por eso el número es aleatorio y no correlativo —si fuera `AGRO-000017` se adivinaría probando desde el uno— y sin letras que se confundan leyendo una etiqueta.

Un despachante ve **lo que tiene**, no lo que existe: `GET /envios/mios` son los que él cargó y todavía no entregó, y por id no puede abrir ninguno más. Sin esto, cualquier despachante se quedaba con la dirección de entrega de todas las compras del sistema.

**El vendedor nunca declara la entrega**, y eso es a propósito: es lo que hace confiable la reseña que después lo califica. El hecho que habilita calificarlo lo escribe alguien que no gana nada con mentir.

Si la entrega es a **coordinar**, no hay despachante ni número: lo declara entregado el **comprador**.

### Reseñas

Una por producto de la orden, del 1 al 5. Sólo la deja el comprador, sólo sobre un producto que esa orden contiene, y sólo cuando el envío figura **ENTREGADO**. No se edita ni se borra.

La calificación de un vendedor (`GET /resenas/vendedor/{usuario}`) se calcula al preguntarla, no se guarda: así no puede quedar desactualizada. Un vendedor sin reseñas devuelve promedio `null`, que no es lo mismo que cero.

De ahí sale su **nivel**, que viaja también en cada tarjeta del catálogo:

| | Hace falta |
|---|---|
| `SIN_CALIFICAR` | todavía no tiene reseñas |
| `BRONCE` | al menos una |
| `ORO` | promedio ≥ 4.0 **y** 5 reseñas |
| `PLATINO` | promedio ≥ 4.5 **y** 10 reseñas |

Cada escalón pide promedio **y** cantidad, porque con el promedio solo una única reseña de 5 estrellas te haría PLATINO — justo lo que el comprador no tiene que creer. Los umbrales están en `application.properties`, así se bajan para una demo sin tocar código.

El nivel de todos los vendedores se resuelve en **una sola consulta** por listado, no una por producto: una página de 100 tarjetas dispara un `GROUP BY`, no cien.

### Ofertas

El comprador propone un precio por unidad, el vendedor acepta o rechaza. Aceptar **cierra la venta**: crea la orden al precio acordado y descuenta el stock.

No reserva stock mientras está pendiente —si no, cualquiera bloquearía el inventario gratis—, así que se valida al aceptar. Vencen a los 7 días. El vendedor decide producto por producto si acepta ofertas; no todo se negocia.

---

## Autenticación

```bash
POST /auth/registro   # crea la cuenta y ya devuelve el token
POST /auth/login      # mail y contraseña a cambio del token
```

Los dos responden `{"access_token": "eyJhbGciOiJIUzUxMiJ9..."}`, y a partir de ahí todo va con `Authorization: Bearer <token>`.

**Qué es ese texto.** Tres partes separadas por puntos: `header.payload.firma`. Las dos primeras son JSON en Base64 y **se leen sin ninguna clave** — pegá un token en jwt.io y vas a ver el mail y el vencimiento en claro. La tercera es `HMAC-SHA512(header+payload, clave)`. O sea que un JWT **no oculta, garantiza que no lo tocaron**: cualquiera puede leerlo, pero sólo el servidor puede fabricar uno. Por eso adentro va el mail y la fecha, nunca la contraseña.

**Qué pasa en cada request.** `JwtAuthenticationFilter` corre antes que cualquier controller: lee el header, verifica la firma, busca el usuario y lo deja en el `SecurityContext`. Si no hay token, o venció, o la firma no cierra, no rechaza nada — deja el contexto vacío y sigue. Quién decide si eso alcanza es `SecurityConfig`, y esa división es lo que permite que convivan rutas públicas y privadas sin un solo `if`.

**El token lleva el mail adentro**, así que cambiarlo invalida el token viejo. Por eso `PUT /usuarios/me` **devuelve un token nuevo** en vez de dejarte deslogueado: el front reemplaza el que tenía guardado y la sesión sigue.

El token dura 24 horas y no se guarda en ninguna tabla: que sea válido se decide verificando la firma y la fecha. Esa es la razón por la que no se puede invalidar uno antes de que venza.

Las contraseñas se guardan con **BCrypt**, que incluye una sal distinta en cada hash y es lento a propósito. Nunca se desencripta: para verificar un login se hashea lo que llega y se comparan los hashes.

---

## Permisos

El id de quien pide sale del **token**, no de la URL. Hay dos reglas: la **pertenencia** pregunta si el recurso es tuyo, y el **rol** pregunta quién sos. Las dos viven en `AutorizacionService`. El ADMIN atraviesa la pertenencia, y esa excepción está dentro de `validarDuenio` y no repartida por los services, para que valga en todos lados por igual.

Son tres roles:

| | Qué hace |
|---|---|
| **CLIENTE** | Compra y vende. Es todo el mundo |
| **ADMIN** | Modera: categorías, fotos, roles, marca las órdenes pagadas y reparte visibilidad. **No comercia** |
| **DESPACHANTE** | Sólo mueve envíos. **No compra ni vende** |

El ADMIN no participa del marketplace, y no es que le falten permisos: le sobran. Un admin que vende puede aprobarse sus propias fotos y despacharse sus propias órdenes; uno que compra audita transacciones en las que es parte.

El despachante **se crea en dos pasos**: se registra como cualquiera y después un ADMIN lo promueve con `PUT /usuarios/{id}/rol?rol=DESPACHANTE`. Y es un camino de ida: `DESPACHANTE → CLIENTE` da 409, porque manejó envíos y vio direcciones de entrega de todo el mundo.

| Operación | Quién |
|---|---|
| Editar, pausar o dar de baja un producto | su vendedor · ADMIN |
| Subir o dar de baja una foto | el vendedor del producto · ADMIN |
| Ver o tocar el carrito y la wishlist | sólo su dueño: la ruta no admite un id ajeno |
| Ver el padrón o los datos de una cuenta ajena | sólo ADMIN |
| Ver el mail, la dirección, el rol o el id de otro | nadie: no salen en ninguna respuesta compartida |
| Ver una orden | comprador · vendedor · ADMIN |
| Marcar una orden PAGADA | **sólo ADMIN** |
| Cancelar una orden PENDIENTE | comprador · vendedor · ADMIN |
| Cancelar una orden PAGADA | nadie, tampoco el ADMIN |
| Despachar un envío | su vendedor |
| Recibirlo y entregarlo | el DESPACHANTE que lo cargó por número |
| Declarar entregado un COORDINAR | el comprador |
| Ver un envío | su comprador, su vendedor, ADMIN · el despachante sólo el suyo en curso |
| Calificar | el comprador, con el envío ENTREGADO |
| Aceptar o rechazar una oferta | el vendedor del producto |
| Repartir visibilidad pagada | sólo ADMIN |
| Crear, editar o dar de baja categorías | ADMIN |
| Comprar un producto | cualquier CLIENTE menos su vendedor |
| Publicar, carritear o comprar | **ADMIN y DESPACHANTE no** |
| Ver el catálogo y crear una cuenta | abierto |

---

## Bajas: nada se borra

Casi nada. **No hay un solo `repository.delete()` en el proyecto.**

| | Cómo se da de baja |
|---|---|
| Usuario, producto, categoría, foto | `activo = false` · `PUT .../baja`, se deshace con `PUT .../reactivar` |
| Orden, oferta | cambian de estado a `CANCELADA` |
| Ítem de carrito, ítem de wishlist | **se borran de verdad** |

Los dos últimos son la única excepción, y es deliberada: un ítem de carrito no es historia de nada, y dejarlo con un flag obligaría a filtrarlo en cada consulta para siempre.

Por eso **`DELETE` significa una sola cosa en toda la API**: esto borra filas. Quedan cuatro, todos de carrito y wishlist. Las bajas lógicas son `PUT .../baja`, haciendo par con `/reactivar`. Así la diferencia se ve en la lista de endpoints sin leer una descripción.

Dar de baja un usuario **arrastra sus publicaciones**: salen del catálogo y de los carritos de todos los demás, para que nadie se entere recién al pagar de que compró algo de un vendedor que ya no está.

---

## Listados: todo paginado

Los 13 listados que pueden crecer sin techo devuelven un sobre, no un array:

```json
{"contenido": [...], "pagina": 0, "tamanio": 20, "total": 143, "totalPaginas": 8}
```

Se pide con `?pagina=` (arranca en 0) y `?tamanio=` (20 por defecto, 100 de tope). **Pedir una página que no existe devuelve la última**, no una lista vacía: el que estaba en la página 9 y perdió resultados no se queda mirando la nada.

Quedan sin paginar los que están acotados por diseño: el árbol de categorías, que el front necesita entero para armar el menú; las fotos de un producto, que van todas en la galería; y los similares, topeados en 8.

**Ningún listado devuelve una lista vacía.** Si no hay resultados sale un 404 con un mensaje que explica qué pasó, porque un `[]` en el front es una pantalla en blanco sin explicación.

---

## Los 71 endpoints

Todo lo que no diga **público** necesita `Authorization: Bearer <token>`.

**Auth**

| | Ruta | Qué hace |
|---|---|---|
| `POST` | `/auth/registro` | Crea la cuenta y devuelve el token · **público** |
| `POST` | `/auth/login` | Mail y contraseña a cambio del token · **público** |

**Usuarios**

| | Ruta | Qué hace |
|---|---|---|
| `GET` | `/usuarios/me` | Quién soy, según el token |
| `PUT` | `/usuarios/me` | Editar mi cuenta. Devuelve un **token nuevo** |
| `PUT` | `/usuarios/me/baja` | Darme de baja. Arrastra mis publicaciones |
| `GET` | `/usuarios` | El padrón · **ADMIN** · paginado |
| `GET` | `/usuarios/{id}` | Una cuenta con todos sus datos · **ADMIN** |
| `PUT` | `/usuarios/{id}/baja` | Dar de baja a otro · **ADMIN** |
| `PUT` | `/usuarios/{id}/reactivar` | Reactivar una cuenta · **ADMIN** |
| `PUT` | `/usuarios/{id}/rol` | Cambiar el rol · **ADMIN** · de despachante a cliente, 409 |

**Categorías**

| | Ruta | Qué hace |
|---|---|---|
| `GET` | `/categorias` | Listar. Con `?soloRaices=true`, sólo las de primer nivel · **público** |
| `GET` | `/categorias/{id}` | Una categoría · **público** |
| `GET` | `/categorias/{id}/subcategorias` | Sus hijas directas · **público** |
| `POST` | `/categorias` | Alta · **ADMIN** |
| `PUT` | `/categorias/{id}` | Editar o mover en el árbol · **ADMIN** |
| `PUT` | `/categorias/{id}/baja` | Baja lógica. 409 si tiene hijas o productos · **ADMIN** |
| `PUT` | `/categorias/{id}/reactivar` | Deshacerla · **ADMIN** |

**Productos**

| | Ruta | Qué hace |
|---|---|---|
| `GET` | `/productos` | El catálogo · **público** · paginado |
| `GET` | `/productos/{id}` | Vista completa. Suma una visita, salvo la del propio vendedor · **público** |
| `GET` | `/productos/{id}/similares` | Hasta 8 parecidos · **público** |
| `GET` | `/productos/vendedor/{usuario}` | La vidriera de un vendedor · **público** · paginado |
| `GET` | `/productos/mis-publicaciones` | Las propias, borradores incluidos · paginado |
| `GET` | `/productos/todos` | Todas las del sistema · **ADMIN** · paginado |
| `POST` | `/productos` | Publicar. Nace en BORRADOR |
| `PUT` | `/productos/{id}` | Editar |
| `PUT` | `/productos/{id}/estado` | PUBLICADO ⇄ PAUSADO |
| `PUT` | `/productos/{id}/baja` | Baja lógica |
| `PUT` | `/productos/{id}/reactivar` | Deshacerla |
| `PUT` | `/productos/{id}/destacar` | Visibilidad paga, por meses · **ADMIN** |

Los filtros del catálogo se combinan entre sí: `idCategoria` (incluye las subcategorías), `nombre`, `precioMin`, `precioMax`, `enOferta`, `provincia`, `condicion`, `anioDesde`, `anioHasta`, `admiteEnvio` y `orden` (`precio_asc`, `precio_desc`, `vistos`, `vendidos`). Sin `?orden=` los destacados van primero; con `?orden=` la visibilidad no interviene, porque si pediste precio la lista tiene que estar por precio.

**Fotos**

| | Ruta | Qué hace |
|---|---|---|
| `GET` | `/fotos?idProducto=` | Las de un producto · **público**, sólo las aprobadas |
| `GET` | `/fotos/{id}` | Metadatos, con lo que dijo la IA · **público**, sólo aprobadas |
| `GET` | `/fotos/{id}/contenido` | Los bytes, para el `src` de un `img` · **público**, sólo aprobadas |
| `GET` | `/fotos/{id}/base64` | Lo mismo, embebido en JSON · **público**, sólo aprobadas |
| `POST` | `/fotos` | Subir. `multipart/form-data` |
| `GET` | `/fotos/pendientes` | La cola de revisión · **ADMIN** · paginado |
| `PUT` | `/fotos/{id}/revision` | Aprobar o rechazar a mano · **ADMIN** |
| `PUT` | `/fotos/{id}/baja` | Baja lógica |

**Carrito y wishlist**

| | Ruta | Qué hace |
|---|---|---|
| `GET` | `/carrito` | El propio, con subtotal y total |
| `POST` | `/carrito/items` | Agregar. Si ya estaba, suma |
| `PUT` | `/carrito/items/{id}` | Cambiar la cantidad. Es absoluta, no un incremento |
| `DELETE` | `/carrito/items/{id}` | Sacar un ítem |
| `DELETE` | `/carrito` | Vaciarlo |
| `GET` | `/wishlist` | La propia |
| `POST` | `/wishlist/items` | Guardar un producto |
| `DELETE` | `/wishlist/items/{id}` | Sacarlo |
| `DELETE` | `/wishlist` | Vaciarla |

**Órdenes**

| | Ruta | Qué hace |
|---|---|---|
| `GET` | `/ordenes/mis-compras` | Lo que compré · paginado |
| `GET` | `/ordenes/mis-ventas` | Lo que vendí · paginado |
| `GET` | `/ordenes/todas` | Todas las del sistema · **ADMIN** · paginado |
| `GET` | `/ordenes/{id}` | Una orden con sus renglones |
| `POST` | `/ordenes` | Cerrar el carrito. Lleva la dirección de entrega |
| `PUT` | `/ordenes/{id}/estado` | PAGADA sólo ADMIN · CANCELADA sólo desde PENDIENTE |

**Envíos**

| | Ruta | Qué hace |
|---|---|---|
| `GET` | `/envios/mios` | Los propios. Un despachante ve lo que tiene en la mano · paginado |
| `GET` | `/envios/todos` | Todos los del sistema · **ADMIN** · paginado |
| `GET` | `/envios/{id}` | Uno, con su seguimiento y sus fechas |
| `GET` | `/envios/historial` | Lo que entregué · **DESPACHANTE** · paginado |
| `POST` | `/envios/recibir?numero=` | Cargar un paquete en la sucursal · **DESPACHANTE** |
| `PUT` | `/envios/{id}/estado` | DESPACHADO el vendedor · ENTREGADO quien lo cargó |

**Ofertas, reseñas y lo demás**

| | Ruta | Qué hace |
|---|---|---|
| `POST` | `/ofertas` | Proponer un precio. Lleva la dirección de entrega |
| `GET` | `/ofertas` | Las que hice y las que recibí · paginado |
| `PUT` | `/ofertas/{id}/aceptar` | Cierra la venta · el vendedor |
| `PUT` | `/ofertas/{id}/rechazar` | El comprador puede volver a ofertar más alto |
| `PUT` | `/ofertas/{id}/cancelar` | Retirarla, sólo mientras siga PENDIENTE |
| `POST` | `/resenas` | Calificar un producto de una orden entregada |
| `GET` | `/resenas/producto/{id}` | Las de un producto · **público** · paginado |
| `GET` | `/resenas/vendedor/{usuario}` | Promedio y cantidad · **público** |
| `GET` | `/notificaciones` | Las propias · paginado |
| `GET` | `/notificaciones/no-leidas` | `{"cantidad": N}`, para el badge de la campanita |
| `PUT` | `/notificaciones/{id}/leida` | Marcarla |
| `GET` | `/dashboard` | El tablero del vendedor |
| `GET` | `/destacados` | Historial de visibilidad paga · paginado |

---

## Los estados

| Enum | Valores |
|---|---|
| `TipoUsuario` | ADMIN · CLIENTE · DESPACHANTE |
| `EstadoPublicacion` | BORRADOR · PUBLICADO · PAUSADO |
| `EstadoVerificacion` | APROBADA · EN_REVISION |
| `EstadoOrden` | PENDIENTE · PAGADA · CANCELADA |
| `MetodoEntrega` | DESPACHO · COORDINAR |
| `EstadoEnvio` | PENDIENTE · DESPACHADO · EN_TRANSITO · ENTREGADO |
| `EstadoOferta` | PENDIENTE · ACEPTADA · RECHAZADA · CANCELADA · VENCIDA |
| `TipoNotificacion` | BAJA_DE_PRECIO · POCO_STOCK · DISPONIBLE_OTRA_VEZ · OFERTA_RECIBIDA · OFERTA_RESPONDIDA |
| `NivelDestacado` | NINGUNO · BASICO · DESTACADO · PREMIUM |
| `CondicionProducto` | NUEVO · USADO |
| `NivelVendedor` | SIN_CALIFICAR · BRONCE · ORO · PLATINO |
| `Provincia` | Las 24 jurisdicciones |

Los enums se guardan **como texto** (`@Enumerated(EnumType.STRING)`), no como número: un `2` en la base no dice nada y se corre si alguien reordena el enum.

---

## Tareas programadas

Cuatro `@Scheduled` barren lo que venció, con su frecuencia configurable en `application.properties`:

| Tarea | Qué hace |
|---|---|
| `LimpiadorDeCarritos` | Vacía los carritos sin tocar por 30 días |
| `LimpiadorDeWishlists` | Lo mismo a los 8 meses: es una lista de intenciones, no una compra en curso |
| `LimpiadorDeOfertas` | Vence las que nadie respondió en 7 días |
| `LimpiadorDeDestacados` | Baja los destacados que se terminaron |

Todas conservan además el chequeo perezoso que había antes —al mirar tu carrito también se valida—, como red por si la tarea no corrió.

---

## Decisiones

**Por qué el ADMIN marca las órdenes como pagadas.** Está arriba, en el flujo de compra: sin pasarela, ninguna de las partes puede probar que el dinero se movió.

**Por qué el vendedor no declara la entrega.** Porque es lo que habilita la reseña que lo califica. Lo escribe el despachante, que no gana nada con mentir.

**Por qué el número de seguimiento es la llave del envío.** Porque es lo único que prueba tener el paquete en la mano. Si hubiera una cola para mirar, pedir el número sería copiar algo de la pantalla.

**Por qué el catálogo trae poco.** Una tarjeta necesita foto, precio y poco más. El resto es una llamada más, sólo cuando alguien abre el producto.

**Por qué las bajas son lógicas.** Una orden vieja tiene que poder mostrar qué se compró aunque el producto ya no se venda.

**Por qué cada ruta dice una sola cosa.** `GET /ordenes` devolvía tus compras, tus ventas, las dos mezcladas o el sistema entero, según tu rol y un parámetro opcional. Ahora son `/mis-compras`, `/mis-ventas` y `/todas`: el nombre alcanza para saber qué pediste, sin leer la documentación. Lo mismo con `/envios/mios` y `/envios/todos`.

**Por qué los ids no viajan en las rutas propias.** `/carrito`, `/wishlist`, `/usuarios/me`, `/dashboard`, `/envios/mios`: si la ruta no admite un id, no hay forma de pedir el de otro. La regla se cumple sola en vez de validarse.

---

## Migraciones

Sólo hacen falta si ya tenías la base cargada de antes. **Si la dropeás y dejás que Hibernate la arme de cero, no corras nada.**

`ddl-auto=update` crea tablas y columnas nuevas, pero **nunca modifica ni borra una que ya existe**. Eso deja dos casos a mano:

**1. Los enums.** Con `@Enumerated(EnumType.STRING)`, Hibernate en MySQL no crea un `VARCHAR` sino un `ENUM(...)` nativo con la lista clavada en el tipo de la columna. Agregar un valor al enum de Java no la cambia, y MySQL tira `Data truncated for column`. Hay que hacerlo a mano:

```sql
ALTER TABLE orden_de_compra MODIFY estado ENUM('PENDIENTE','PAGADA','CANCELADA') NOT NULL;
ALTER TABLE envio MODIFY estado ENUM('PENDIENTE','DESPACHADO','EN_TRANSITO','ENTREGADO') NOT NULL;
ALTER TABLE usuario MODIFY rol ENUM('ADMIN','CLIENTE','DESPACHANTE') NOT NULL;
```

**2. Las columnas NOT NULL sobre tablas con filas.** MySQL tiene que inventar un valor para lo que ya está, y para un ENUM inventa `''`, que no es válido. Se agregan nullable, se completan y recién ahí se marcan:

```sql
ALTER TABLE producto
  ADD COLUMN provincia ENUM('BUENOS_AIRES','CABA','CATAMARCA','CHACO','CHUBUT','CORDOBA',
    'CORRIENTES','ENTRE_RIOS','FORMOSA','JUJUY','LA_PAMPA','LA_RIOJA','MENDOZA','MISIONES',
    'NEUQUEN','RIO_NEGRO','SALTA','SAN_JUAN','SAN_LUIS','SANTA_CRUZ','SANTA_FE',
    'SANTIAGO_DEL_ESTERO','TIERRA_DEL_FUEGO','TUCUMAN') NULL,
  ADD COLUMN condicion ENUM('NUEVO','USADO') NULL,
  ADD COLUMN anio INT NULL;

UPDATE producto SET provincia='BUENOS_AIRES', condicion='USADO', anio=2020
 WHERE provincia IS NULL;

ALTER TABLE producto
  MODIFY provincia ENUM(/* las 24 */) NOT NULL,
  MODIFY condicion ENUM('NUEVO','USADO') NOT NULL,
  MODIFY anio INT NOT NULL;
```

Y una columna que se fue, porque la dirección ahora vive en la orden:

```sql
ALTER TABLE envio DROP COLUMN direccion_entrega;
ALTER TABLE envio ADD UNIQUE KEY uk_envio_seguimiento (numero_seguimiento);
```

---

## Lo que queda pendiente

- **Paginación en la base.** El sobre está, pero el corte se hace sobre la lista ya armada: la consulta sigue trayendo todo. Con miles de productos habría que llevar los filtros y el orden a SQL.
- **WebP.** `ImageIO` no lo lee, así que esas subidas no llegan a Gemini y quedan `EN_REVISION`. Desde que las no aprobadas no se muestran, el agujero se cerró, pero el formato sigue sin funcionar.
- **Historial de estados.** Cada orden guarda cuándo se creó y cuándo cambió por última vez, pero no el camino completo. Para eso haría falta una tabla aparte.
- **El rodeo de dos pasos en los roles.** Un ADMIN puede hacer `DESPACHANTE → ADMIN → CLIENTE`, porque la regla mira el rol de hoy. Cerrarlo pide guardar un "fue despachante" en el usuario.
- **Qué lleva cada envío.** El historial del despachante dice qué entregó; el envío en curso todavía no, y tampoco de dónde retirarlo.
