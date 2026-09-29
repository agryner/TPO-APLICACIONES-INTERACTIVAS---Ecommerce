package com.uade.tpo.marketplace.controllers.common;

import java.util.List;

import lombok.Data;

@Data
public class PaginaResponse<T> {
    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long total;
    private int totalPaginas;

    /**
     * Pre : la lista COMPLETA ya filtrada y ordenada, que pagina se pidio y de
     *       que tamanio. Una pagina negativa se toma como la primera y un
     *       tamanio fuera de rango se acomoda solo: son parametros que escribe
     *       el front, no vale romper por un numero raro.
     * Post: esa pagina, con lo que hace falta para dibujar un paginador y nada
     *       mas. Pedir una pagina que no existe devuelve la ultima, asi el que
     *       estaba en la pagina 9 y perdio resultados no se queda mirando una
     *       lista vacia.
     */
    public static <T> PaginaResponse<T> de(List<T> todo, Integer pagina, Integer tamanio) {
        int size = tamanio == null || tamanio < 1 ? TAMANIO_POR_DEFECTO
                : Math.min(tamanio, TAMANIO_MAXIMO);
        int totalPaginas = Math.max(1, (int) Math.ceil(todo.size() / (double) size));
        int page = pagina == null || pagina < 0 ? 0 : Math.min(pagina, totalPaginas - 1);

        int desde = page * size;
        int hasta = Math.min(desde + size, todo.size());

        PaginaResponse<T> dto = new PaginaResponse<>();
        dto.setContenido(desde >= todo.size() ? List.of() : todo.subList(desde, hasta));
        dto.setPagina(page);
        dto.setTamanio(size);
        dto.setTotal(todo.size());
        dto.setTotalPaginas(totalPaginas);
        return dto;
    }

    public static final int TAMANIO_POR_DEFECTO = 20;
    public static final int TAMANIO_MAXIMO = 100;
}
