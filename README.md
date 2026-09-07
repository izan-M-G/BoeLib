# boe-client

[![](https://jitpack.io/v/izan-M-G/BoeLib.svg)](https://jitpack.io/#izan-M-G/BoeLib)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Cliente Java para la API de datos abiertos del BOE.

## Por qué existe

La API del BOE devuelve el sumario diario en una estructura irregular: el mismo campo llega
unas veces como objeto y otras como lista, aparecen envoltorios `texto` en tres niveles
distintos, y hay varios caminos posibles desde una sección hasta un documento.

Esta librería absorbe esa complejidad y entrega una lista plana de disposiciones con tipos
Java de verdad.

## Instalación

Añade el repositorio de JitPack y la dependencia:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.izanMG</groupId>
    <artifactId>boe-client</artifactId>
    <version>v0.1.1</version>
</dependency>
```

Con Gradle:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.izanMG:boe-client:v0.1.1'
}
```

Requiere Java 21 o superior.

## Uso

### Obtener el sumario de un día

```java
BoeClient boe = BoeClient.create();

boe.sumarioDe(LocalDate.of(2026, 9, 2)).ifPresent(sumario -> {
    for (Diario diario : sumario.diarios()) {
        System.out.println("Boletín número " + diario.numero());
        for (Disposicion d : diario.disposiciones()) {
            System.out.println("  " + d.identificador() + " - " + d.titulo());
        }
    }
});
```

### Filtrar por sección

```java
BoeClient boe = BoeClient.create();

List<Disposicion> oposiciones = boe.sumarioDe(LocalDate.of(2026, 9, 2))
        .stream()
        .flatMap(s -> s.diarios().stream())
        .flatMap(d -> d.disposiciones().stream())
        .filter(d -> d.seccion() == Seccion.OPOSICIONES)
        .toList();
```

### Días sin boletín

El BOE no se publica todos los días. `sumarioDe` devuelve un `Optional` vacío en lugar de
lanzar una excepción:

```java
Optional<Sumario> sumario = boe.sumarioDe(LocalDate.of(2026, 8, 30)); // domingo

if (sumario.isEmpty()) {
    System.out.println("Ese día no hubo boletín");
}
```

### Configuración

```java
BoeClient boe = BoeClient.builder()
        .timeout(Duration.ofSeconds(60))
        .reintentos(5)
        .build();
```

## Qué hace

- Consulta el sumario del BOE de cualquier fecha (el histórico llega al menos a 1990).
- Normaliza la estructura irregular de la respuesta.
- Devuelve las disposiciones como una lista plana, cada una con su sección, departamento
  y epígrafe ya resueltos.
- Reintenta automáticamente ante errores temporales del servidor, con espera creciente.

## Qué no hace

- No descarga ni procesa los PDF: devuelve el enlace.
- No busca por texto libre dentro de las disposiciones.
- No consulta la API de legislación consolidada ni las tablas auxiliares.
- No cachea resultados.

## Decisiones técnicas

### La envoltura `texto`

El JSON de la API es una conversión desde XML. Cuando un nodo tiene un único hijo, el
conversor lo envuelve en un objeto `texto` en lugar de dejarlo directo. Aparece en tres
niveles distintos y con contenido diferente en cada uno:

| Dónde aparece | Qué contiene |
|---|---|
| dentro de `seccion` | `departamento` |
| dentro de `departamento` | `epigrafe` |
| dentro de `departamento` | `item` |

La librería recoge los elementos vengan por donde vengan, de modo que ese envoltorio no
existe en el modelo público.

### Objeto o lista según la cantidad

Por la misma conversión desde XML, un campo con varios elementos llega como array y con uno
solo como objeto suelto. Se resuelve activando `ACCEPT_SINGLE_VALUE_AS_ARRAY` en Jackson y
declarando siempre listas en los DTO.

### Un 404 no es un error

Los domingos normalmente no hay boletín, y la API responde 404. Como eso es comportamiento
normal y no un fallo, `sumarioDe` devuelve un `Optional` vacío en vez de lanzar una
excepción. Las excepciones quedan reservadas para lo que sí es un problema: fallos de red,
errores del servidor y respuestas malformadas.

Curiosidad: aunque la petición pide JSON, los errores 4xx llegan con el cuerpo en XML. La
librería decide por el código de estado HTTP y nunca parsea el cuerpo de un error.

### DTO separados del modelo público

Hay dos modelos de los mismos datos:

- Los **DTO internos** son fieles al JSON, con toda su irregularidad. No llevan lógica.
- El **modelo público** (`Sumario`, `Diario`, `Disposicion`, `Seccion`) es limpio: fechas
  como `LocalDate`, secciones como enum, epígrafes como `Optional`, nunca `null`.

Entre ambos hay un mapeador que actúa como capa anticorrupción. Si el BOE cambia el formato,
solo hay que tocar esa capa y quien use la librería no se entera.

### Secciones desconocidas

El enum `Seccion` incluye un valor `DESCONOCIDA`. Si el BOE introduce un código nuevo, la
librería lo devuelve como desconocido y sigue funcionando, en lugar de romperse por un
cambio que no es asunto de quien la usa.

## Tests

La librería está validada contra respuestas reales de la API guardadas como fixtures, que
cubren un día corriente, un día con dos boletines, un extraordinario de domingo y un sumario
de 1990. Los tests de red usan un servidor HTTP simulado, así que la suite no depende de que
el BOE esté disponible.

Ejecutar los tests:

```bash
mvn test
```

Hay además un test de integración contra la API real, excluido del build normal:

```bash
mvn test -DexcludedGroups= -Dgroups=integration
```

## Atribución

Los datos proceden de la API de datos abiertos de la Agencia Estatal Boletín Oficial del
Estado. Su reutilización está sujeta a las condiciones publicadas en
<https://www.boe.es/avisos_legales/>.

Este proyecto no está afiliado a la Agencia Estatal BOE.

## Licencia

MIT. Ver [LICENSE](LICENSE).
