package io.github.izanmg.boe;

public enum Seccion {

    DISPOSICIONES_GENERALES("1"),
    NOMBRAMIENTOS("2A"),
    OPOSICIONES("2B"),
    OTRAS_DISPOSICIONES("3"),
    ADMINISTRACION_DE_JUSTICIA("4"),
    ANUNCIOS_CONTRATACION("5A"),
    ANUNCIOS_OFICIALES("5B"),
    ANUNCIOS_PARTICULARES("5C"),
    DESCONOCIDA("");

    private final String codigo;

     Seccion (String codigo){
         this.codigo = codigo;
    }

    public String codigo(){
         return codigo;
    }

    public static Seccion desdeCodigo(String codigo) {
        if (codigo == null) {
            return DESCONOCIDA;
        }
        for (Seccion s : values()) {
            if (s.codigo.equalsIgnoreCase(codigo)) {
                return s;
            }
        }
        return DESCONOCIDA;
    }



}
