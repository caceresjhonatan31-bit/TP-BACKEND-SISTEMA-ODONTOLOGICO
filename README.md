# Plataforma de Gestión de Turnos Odontológicos 🦷

Este repositorio contiene la arquitectura de microservicios para la plataforma de gestión de turnos odontológicos.
Este documento detalla **todo lo necesario para inicializar, configurar y ejecutar tanto los microservicios Spring Boot como las bases de datos de desarrollo local** para cualquier miembro del equipo.

---

## 📋 Tabla de Contenidos

1. [Arquitectura del Sistema](#-arquitectura-del-sistema)
2. [Requisitos Previos](#-requisitos-previos)
3. [Mapa de Puertos y Servicios](#-mapa-de-puertos-y-servicios)
4. [Inicio Rápido (Guía de Ejecución Local)](#-inicio-rápido-guía-de-ejecución-local)
   - [Paso 1: Levantar las Bases de Datos (Docker)](#paso-1-levantar-las-bases-de-datos-docker)
   - [Paso 2: Compilar e Instalar `common-excepciones` (¡Obligatorio!)](#paso-2-compilar-e-instalar-common-excepciones-obligatorio)
   - [Paso 3: Ejecutar los Microservicios y Gateway](#paso-3-ejecutar-los-microservicios-y-gateway)
5. [Documentación y Swagger UI](#-documentación-y-swagger-ui)
6. [Credenciales y Datos de Conexión de Bases de Datos](#-credenciales-y-datos-de-conexión-de-bases-de-datos)
7. [Estructura del Proyecto y Esquemas SQL](#-estructura-del-proyecto-y-esquemas-sql)
8. [Cómo Conectarse a las Bases de Datos](#-cómo-conectarse-a-las-bases-de-datos)
   - [Desde Cliente Gráfico](#opción-1-cliente-gráfico-dbeaver-pgadmin-datagrip-o-extensión-de-ide)
   - [Desde la Terminal con Docker CLI (psql)](#opción-2-conexión-por-consola-vía-docker-exec)
9. [Configuración en los Microservicios (Spring Boot)](#-configuración-en-los-microservicios-spring-boot)
10. [Comandos Frecuentes de Gestión](#-comandos-frecuentes-de-gestión)
11. [Reinicio Limpio / Reset de Datos (Importante)](#-reinicio-limpio--reset-de-datos-importante)
12. [Resolución de Problemas Comunes (Troubleshooting)](#-resolución-de-problemas-comunes-troubleshooting)

---

## 🏛 Arquitectura del Sistema

El sistema implementa una arquitectura basada en **Microservicios** con **Spring Boot 3.4.2** y **Java 21**, complementada con el patrón **Database-per-Service** utilizando **PostgreSQL 16**:

1. **`api-gateway`** (Spring Cloud Gateway - Puerto `8080`):
   - Punto de entrada unificado y reactivo (Spring WebFlux / Netty). Enruta las peticiones de los clientes hacia los microservicios correspondientes:
     - `/api/turnos/**` y `/api/usuarios/**` ➔ `ms-turno-usuarios` (`http://localhost:8081`)
     - `/api/pacientes/**` y `/api/practicas/**` ➔ `ms-pacientes-practicas` (`http://localhost:8082`)
2. **`ms-turno-usuarios`** (Spring Boot Web / JPA - Puerto `8081`):
   - Gestiona usuarios del consultorio (Administradores, Odontólogos, Recepcionistas), sus disponibilidades horarias y la agenda/estado de los turnos. Se conecta a `db_turno_usuarios` (Puerto `5433`).
3. **`ms-pacientes-practicas`** (Spring Boot Web / JPA - Puerto `8082`):
   - Gestiona la ficha de los pacientes, su historial de evoluciones clínicas y el nomenclador de prácticas odontológicas. Se conecta a `db_pacientes_practicas` (Puerto `5434`).
4. **`common-excepciones`** (Librería Maven compartida):
   - Provee manejo unificado de errores HTTP con `@RestControllerAdvice` (`GlobalExceptionHandler`), DTO estandarizado (`ErrorResponseDTO`) y excepciones comunes de negocio (`ResourceNotFoundException`, `ConflictException`, `ExternalServiceException`).

> **Nota de Integridad Referencial:**  
> Al estar desacopladas físicamente, las referencias entre ambas bases de datos son **lógicas** (por IDs). Por ejemplo, un turno en `db_turno_usuarios` guarda `paciente_id` que corresponde a un registro en `db_pacientes_practicas`, pero no existe una foreign key física de base de datos entre ellas.

---

## ⚙ Requisitos Previos

Antes de comenzar, asegúrate de tener instalado y configurado en tu equipo:

- **Java Development Kit (JDK) 21** (recomendado Eclipse Temurin, Amazon Corretto u OpenJDK 21). Verifica con:
  ```bash
  java -version
  ```
- **Apache Maven 3.9+** (o el Maven integrado en tu IDE). Verifica con:
  ```bash
  mvn -version
  ```
- **Docker Desktop** (en Windows/Mac) o **Docker Engine + Docker Compose** (en Linux).
- Terminal de comandos (PowerShell, Bash o Zsh).
- Opcional: Un cliente SQL gráfico como [DBeaver](https://dbeaver.io/), [pgAdmin](https://www.pgadmin.org/), DataGrip o la extensión *Database Client* en VS Code.

---

## 🗺 Mapa de Puertos y Servicios

| Componente | Tipo / Framework | Puerto Host | Descripción | Documentación / Acceso |
| :--- | :--- | :--- | :--- | :--- |
| **`api-gateway`** | Spring Cloud Gateway | `8080` | Router reactivo y punto de acceso central | `http://localhost:8080` |
| **`ms-turno-usuarios`** | Spring Boot 3.4.2 | `8081` | Agenda, turnos, usuarios y disponibilidades | [Swagger UI](http://localhost:8081/swagger-ui.html) |
| **`ms-pacientes-practicas`** | Spring Boot 3.4.2 | `8082` | Pacientes, evoluciones y prácticas | [Swagger UI](http://localhost:8082/swagger-ui.html) |
| **`common-excepciones`** | Librería Java / JAR | N/A | Manejo global de excepciones (`.m2`) | Dependencia compartida |
| **`db_turno_usuarios`** | PostgreSQL 16 (Docker) | `5433` | Base de datos de Turnos y Usuarios | `localhost:5433` |
| **`db_pacientes_practicas`** | PostgreSQL 16 (Docker) | `5434` | Base de datos de Pacientes y Prácticas | `localhost:5434` |

---

## 🚀 Inicio Rápido (Guía de Ejecución Local)

Sigue estos 3 pasos en orden:

### Paso 1: Levantar las Bases de Datos (Docker)

1. Abre una terminal en la raíz del proyecto (`plataforma-turnos-odontologicos`).
2. Ejecuta:
   ```bash
   # Opción estándar
   docker compose up -d

   # O bien usando explícitamente el archivo de desarrollo
   docker compose -f docker-compose.dev.yml up -d
   ```
3. Verifica que ambos contenedores estén en estado `running`:
   ```bash
   docker compose ps
   ```

### Paso 2: Compilar e Instalar `common-excepciones` (¡Obligatorio!)

> ⚠️ **¡PASO CRÍTICO PARA EL EQUIPO!**  
> Los microservicios tienen a `common-excepciones` declarada como dependencia Maven:
> ```xml
> <dependency>
>     <groupId>com.consultorio</groupId>
>     <artifactId>common-excepciones</artifactId>
>     <version>1.0.0</version>
> </dependency>
> ```
> Dado que no existe un POM agregador raíz, **es obligatorio compilar e instalar esta librería en el repositorio local (`.m2`) antes de compilar o arrancar los microservicios**. Si se omite este paso, Maven arrojará el error `Could not find artifact com.consultorio:common-excepciones:jar:1.0.0`.

Desde la terminal, ejecuta:

```bash
cd common-excepciones
mvn clean install
cd ..
```

### Paso 3: Ejecutar los Microservicios y Gateway

Puedes ejecutar cada proyecto desde tu IDE (ejecutando la clase Application con `main`) o abriendo terminales independientes para cada componente:

#### Terminal 1 — Microservicio de Turnos y Usuarios (Puerto 8081)
```bash
cd ms-turno-usuarios
mvn spring-boot:run
```

#### Terminal 2 — Microservicio de Pacientes y Prácticas (Puerto 8082)
```bash
cd ms-pacientes-practicas
mvn spring-boot:run
```

#### Terminal 3 — API Gateway (Puerto 8080)
```bash
cd api-gateway
mvn spring-boot:run
```

---

## 📖 Documentación y Swagger UI

Cada microservicio cuenta con interfaz interactiva OpenAPI / Swagger UI integrada:

- **Swagger UI Turnos y Usuarios:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)  
  *(OpenAPI JSON: `http://localhost:8081/v3/api-docs`)*
- **Swagger UI Pacientes y Prácticas:** [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)  
  *(OpenAPI JSON: `http://localhost:8082/v3/api-docs`)*

---

## 🔑 Credenciales y Datos de Conexión de Bases de Datos

| Parámetro | Base de Datos: Turnos y Usuarios | Base de Datos: Pacientes y Prácticas |
| :--- | :--- | :--- |
| **Microservicio** | `ms-turno-usuarios` | `ms-pacientes-practicas` |
| **Nombre del Contenedor** | `pg_turno_usuarios` | `pg_pacientes_practicas` |
| **Host** | `localhost` / `127.0.0.1` | `localhost` / `127.0.0.1` |
| **Puerto en Host (Tu PC)** | **`5433`** | **`5434`** |
| **Puerto Interno Docker** | `5432` | `5432` |
| **Database Name** | `db_turno_usuarios` | `db_pacientes_practicas` |
| **Usuario** | `postgres` | `postgres` |
| **Contraseña** | `password123` | `password123` |
| **Script DDL Inicial** | `./database/init-turno-usuarios.sql` | `./database/init-pacientes-practicas.sql` |
| **Carpeta Persistencia** | `./data/turno-usuarios` | `./data/pacientes-practicas` |
| **JDBC URL** | `jdbc:postgresql://localhost:5433/db_turno_usuarios` | `jdbc:postgresql://localhost:5434/db_pacientes_practicas` |

---

## 📁 Estructura del Proyecto y Esquemas SQL

```text
plataforma-turnos-odontologicos/
├── api-gateway/                         # Enrutador principal (Spring Cloud Gateway, puerto 8080)
│   ├── src/main/java/com/consultorio/gateway/ApiGatewayApplication.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── common-excepciones/                  # Componente común reutilizable (librería jar)
│   ├── src/main/java/com/consultorio/common/
│   │   ├── dto/ErrorResponseDTO.java
│   │   ├── exception/ (ConflictException, ExternalServiceException, ResourceNotFoundException)
│   │   └── handler/GlobalExceptionHandler.java
│   └── pom.xml
├── ms-turno-usuarios/                   # Microservicio de Turnos y Usuarios (puerto 8081)
│   ├── src/main/java/com/consultorio/turnousuarios/MsTurnoUsuariosApplication.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── ms-pacientes-practicas/              # Microservicio de Pacientes y Prácticas (puerto 8082)
│   ├── src/main/java/com/consultorio/pacientespracticas/MsPacientesPracticasApplication.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── database/
│   ├── init-turno-usuarios.sql          # Tablas, índices y constraints de Turnos y Usuarios
│   └── init-pacientes-practicas.sql     # Tablas, índices y constraints de Pacientes y Prácticas
├── data/                                # Datos físicos generados por Postgres (ignorado en git)
│   ├── turno-usuarios/
│   └── pacientes-practicas/
├── docker-compose.dev.yml               # Configuración de los contenedores Docker
├── docker-compose.yml                   # Configuración compose por defecto
└── README.md                            # Guía del proyecto
```

### Tablas en `db_turno_usuarios`
- `usuarios`: Administradores, odontólogos y recepcionistas. Contiene `keycloak_id`, `dni`, `email`, `rol`, `matricula` y `activo`.
- `disponibilidad_odontologos`: Días de la semana (`LUNES`..`DOMINGO`) y rangos horarios (`hora_inicio`, `hora_fin`) de cada odontólogo.
- `turnos`: Fecha, horario, estado (`PROGRAMADO`, `ATENDIDO`, `INASISTIDO`, `CANCELADO`), nota y referencias al odontólogo y paciente.
- `turno_practicas_realizadas`: Historial de prácticas efectuadas asociadas a la consulta.

### Tablas en `db_pacientes_practicas`
- `pacientes`: Ficha general de pacientes (`dni`, `nombre`, `apellido`, `telefono`, `email`, `activo`, `fecha_alta`).
- `evoluciones_clinicas`: Diagnósticos y texto de notas clínicas vinculados al historial de turnos y al odontólogo responsable.
- `practicas_odontologicas`: Catálogo del nomenclador odontológico (`codigo_nomenclador`, `nombre`, `descripcion`, `activo`).

---

## 🔌 Cómo Conectarse a las Bases de Datos

### Opción 1: Cliente Gráfico (DBeaver, pgAdmin, DataGrip o Extensión de IDE)

Crea dos conexiones de tipo **PostgreSQL** con los siguientes datos:

#### Conexión 1 (Turnos y Usuarios):
- **Host**: `localhost`
- **Port**: `5433`
- **Database**: `db_turno_usuarios`
- **Username**: `postgres`
- **Password**: `password123`

#### Conexión 2 (Pacientes y Prácticas):
- **Host**: `localhost`
- **Port**: `5434`
- **Database**: `db_pacientes_practicas`
- **Username**: `postgres`
- **Password**: `password123`

---

### Opción 2: Conexión por Consola vía `docker exec`

No necesitas tener PostgreSQL instalado en tu máquina host para interactuar con la terminal `psql`:

```bash
# Ingresar a la base de Turnos y Usuarios
docker exec -it pg_turno_usuarios psql -U postgres -d db_turno_usuarios

# Ingresar a la base de Pacientes y Prácticas
docker exec -it pg_pacientes_practicas psql -U postgres -d db_pacientes_practicas
```

Una vez dentro de `psql`, puedes usar:
- `\dt` para listar todas las tablas creadas.
- `\d nombre_tabla` para ver la estructura de una tabla.
- `\q` para salir.

---

## ☕ Configuración en los Microservicios (Spring Boot)

En los archivos `application.yml` de cada servicio, los datasources ya se encuentran preconfigurados:

### En `ms-turno-usuarios` (`src/main/resources/application.yml`):
```yaml
server:
  port: 8081

spring:
  application:
    name: ms-turno-usuarios
  datasource:
    url: jdbc:postgresql://127.0.0.1:5433/db_turno_usuarios
    username: postgres
    password: password123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # Valida contra las tablas ya creadas por init.sql
    show-sql: true
    properties:
      hibernate:
        format_sql: true

clients:
  pacientes-practicas:
    url: http://localhost:8082
  feriados-api:
    url: https://api.argentinadatos.com/v1/feriados
```

### En `ms-pacientes-practicas` (`src/main/resources/application.yml`):
```yaml
server:
  port: 8082

spring:
  application:
    name: ms-pacientes-practicas
  datasource:
    url: jdbc:postgresql://127.0.0.1:5434/db_pacientes_practicas
    username: postgres
    password: password123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # Valida contra las tablas ya creadas por init.sql
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

---

## 🛠 Comandos Frecuentes de Gestión

| Acción | Comando |
| :--- | :--- |
| **Iniciar contenedores de DB** | `docker compose -f docker-compose.dev.yml up -d` |
| **Detener contenedores (sin borrar datos)** | `docker compose -f docker-compose.dev.yml stop` |
| **Reanudar contenedores detenidos** | `docker compose -f docker-compose.dev.yml start` |
| **Reiniciar contenedores** | `docker compose -f docker-compose.dev.yml restart` |
| **Bajar contenedores** | `docker compose -f docker-compose.dev.yml down` |
| **Ver logs del servicio de turnos** | `docker compose -f docker-compose.dev.yml logs -f db-turno-usuarios` |
| **Ver logs del servicio de pacientes** | `docker compose -f docker-compose.dev.yml logs -f db-pacientes-practicas` |
| **Compilar módulo común** | `cd common-excepciones && mvn clean install && cd ..` |
| **Compilar todo un microservicio** | `cd <nombre-modulo> && mvn clean package && cd ..` |

---

## 🔄 Reinicio Limpio / Reset de Datos (Importante)

> ⚠️ **¿Cómo funciona la inicialización de Postgres en Docker?**  
> Los scripts montados en `/docker-entrypoint-initdb.d/init.sql` **sólo se ejecutan la primera vez que el directorio `/var/lib/postgresql/data` está completamente vacío**.  
> Si modificas los archivos `.sql` de la carpeta `./database/` o necesitas restablecer la base de datos limpia de fábrica, debes eliminar el contenido de la carpeta `./data/`.

### Pasos para reiniciar desde cero:

1. **Detener y remover los contenedores:**
   ```bash
   docker compose -f docker-compose.dev.yml down
   ```

2. **Eliminar los datos locales acumulados:**

   - **En Windows (PowerShell):**
     ```powershell
     Remove-Item -Recurse -Force ./data/turno-usuarios/*, ./data/pacientes-practicas/*
     ```
   - **En Linux / macOS (Bash):**
     ```bash
     rm -rf ./data/turno-usuarios/* ./data/pacientes-practicas/*
     ```

3. **Volver a levantar los contenedores:**
   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```
   Postgres detectará que las carpetas están vacías y ejecutará nuevamente los scripts `init-turno-usuarios.sql` e `init-pacientes-practicas.sql`.

---

## ❓ Resolución de Problemas Comunes (Troubleshooting)

### 1. `Could not find artifact com.consultorio:common-excepciones:jar:1.0.0`
- **Causa:** Intentaste compilar o iniciar `ms-pacientes-practicas` o `ms-turno-usuarios` sin haber compilado e instalado previamente la librería común.
- **Solución:**
  ```bash
  cd common-excepciones
  mvn clean install
  cd ..
  ```

### 2. `Error: bind: address already in use` (puerto ocupado)
- **Causa:** Tienes una instancia local de PostgreSQL u otro servicio corriendo en los puertos `5433` o `5434`, o una aplicación usando los puertos `8080`, `8081` u `8082`.
- **Solución:**
  - Verifica qué proceso está usando el puerto:
    - En Windows: `netstat -ano | findstr :<puerto>`
    - En Linux/Mac: `lsof -i :<puerto>`
  - Puedes cerrar el servicio conflictivo o cambiar los puertos asignados en `docker-compose.dev.yml` o en el `application.yml` correspondiente.

### 3. Error de conexión con la base de datos (`Connection refused`)
- **Causa:** Los contenedores de Docker no están corriendo antes de iniciar la aplicación Spring Boot.
- **Solución:** Asegúrate de ejecutar `docker compose up -d` y esperar unos segundos hasta que Postgres acepte conexiones antes de arrancar los microservicios.

### 4. El IDE muestra errores de importación en las clases Spring Boot
- **Causa:** La caché del servidor de lenguaje del IDE (VS Code / Eclipse / IntelliJ) no ha sincronizado las dependencias descargadas por Maven.
- **Solución:**
  - En VS Code / Antigravity: Abre la paleta de comandos (`Ctrl + Shift + P`) y ejecuta **"Java: Clean Java Language Server Workspace"** o haz clic derecho en el proyecto y elige **Maven > Reload Project**.
  - Ejecuta `mvn clean compile` dentro de la carpeta del microservicio desde la terminal para forzar la resolución.

### 5. No se crearon las tablas o no se ejecutó el script `.sql`
- **Causa:** La carpeta `./data/turno-usuarios` o `./data/pacientes-practicas` ya tenía datos inicializados previamente.
- **Solución:** Sigue los pasos de [Reinicio Limpio / Reset de Datos](#-reinicio-limpio--reset-de-datos-importante).

### 6. Error `password authentication failed for user "postgres"`
- **Causa:** Existían datos previos con otra contraseña en la carpeta `./data/`.
- **Solución:** Elimina los datos existentes en `./data/` y vuelve a levantar el compose.

### 7. `docker: Cannot connect to the Docker daemon`
- **Causa:** Docker Desktop no está iniciado o el servicio de Docker está detenido.
- **Solución:** Inicia la aplicación Docker Desktop y espera a que el icono figure en verde ("Engine running").
