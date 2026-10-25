# Plataforma de Gestión de Turnos Odontológicos 🦷

Este repositorio contiene la arquitectura de microservicios para la plataforma de gestión de turnos odontológicos.
Este documento detalla **todo lo necesario para inicializar, configurar y gestionar las bases de datos de desarrollo local** para cualquier miembro del equipo.

---

## 📋 Tabla de Contenidos

1. [Arquitectura de Datos](#-arquitectura-de-datos)
2. [Requisitos Previos](#-requisitos-previos)
3. [Credenciales y Datos de Conexión](#-credenciales-y-datos-de-conexión)
4. [Inicio Rápido (Levantar las Bases de Datos)](#-inicio-rápido-levantar-las-bases-de-datos)
5. [Estructura del Proyecto y Esquemas SQL](#-estructura-del-proyecto-y-esquemas-sql)
6. [Cómo Conectarse a las Bases de Datos](#-cómo-conectarse-a-las-bases-de-datos)
   - [Desde Cliente Gráfico (DBeaver, pgAdmin, VS Code)](#opción-1-cliente-gráfico-dbeaver-pgadmin-datagrip-o-extensión-de-ide)
   - [Desde la Terminal con Docker CLI (psql)](#opción-2-conexión-por-consola-vía-docker-exec)
7. [Configuración en los Microservicios (Spring Boot)](#-configuración-en-los-microservicios-spring-boot)
8. [Comandos Frecuentes de Gestión](#-comandos-frecuentes-de-gestión)
9. [Reinicio Limpio / Reset de Datos (Importante)](#-reinicio-limpio--reset-de-datos-importante)
10. [Resolución de Problemas Comunes (Troubleshooting)](#-resolución-de-problemas-comunes-troubleshooting)

---

## 🏛 Arquitectura de Datos

El sistema implementa el patrón **Database-per-Service** (una base de datos independiente por cada microservicio) utilizando **PostgreSQL 16**:

1. **`db_turno_usuarios`** (Puerto Host `5433`):
   - Gestiona usuarios del consultorio (Administradores, Odontólogos, Recepcionistas), sus disponibilidades horarias y la agenda/estado de los turnos.
2. **`db_pacientes_practicas`** (Puerto Host `5434`):
   - Gestiona la ficha de los pacientes, su historial de evoluciones clínicas y el nomenclador de prácticas odontológicas.

> **Nota de Integridad Referencial:**  
> Al estar desacopladas físicamente, las referencias entre ambas bases de datos son **lógicas** (por IDs). Por ejemplo, un turno en `db_turno_usuarios` guarda `paciente_id` que corresponde a un registro en `db_pacientes_practicas`, pero no existe una foreign key física de base de datos entre ellas.

---

## ⚙ Requisitos Previos

Antes de comenzar, asegúrate de tener instalado y en funcionamiento:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (en Windows/Mac) o [Docker Engine + Docker Compose](https://docs.docker.com/engine/install/) (en Linux).
- Terminal de comandos (PowerShell, Bash o Zsh).
- Opcional: Un cliente SQL como [DBeaver](https://dbeaver.io/), [pgAdmin](https://www.pgadmin.org/), DataGrip o la extensión *Database Client* en VS Code.

---

## 🔑 Credenciales y Datos de Conexión

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

## 🚀 Inicio Rápido (Levantar las Bases de Datos)

1. Abre una terminal en la raíz del proyecto (`plataforma-turnos-odontologicos`).
2. Ejecuta cualquiera de los dos comandos (ambos son válidos):

```bash
# Opción estándar (usa docker-compose.yml por defecto)
docker compose up -d

# Opción explícita de desarrollo
docker compose -f docker-compose.dev.yml up -d
```

*(En versiones antiguas de Docker Compose v1 puedes usar `docker-compose up -d`)*

3. Verifica que ambos contenedores estén en estado `running`:

```bash
docker compose ps
```

Deberías ver una salida similar a:

```text
NAME                     IMAGE                COMMAND                  SERVICE                  CREATED         STATUS         PORTS
pg_pacientes_practicas   postgres:16-alpine   "docker-entrypoint.s…"   db-pacientes-practicas   5 seconds ago   Up 4 seconds   0.0.0.0:5434->5432/tcp
pg_turno_usuarios        postgres:16-alpine   "docker-entrypoint.s…"   db-turno-usuarios        5 seconds ago   Up 4 seconds   0.0.0.0:5433->5432/tcp
```

4. Ver logs en tiempo real (opcional):

```bash
docker compose -f docker-compose.dev.yml logs -f
```

---

## 📁 Estructura del Proyecto y Esquemas SQL

```text
plataforma-turnos-odontologicos/
├── database/
│   ├── init-turno-usuarios.sql          # Tablas, índices y constraints de Turnos y Usuarios
│   └── init-pacientes-practicas.sql     # Tablas, índices y constraints de Pacientes y Prácticas
├── data/                                # Datos físicos generados por Postgres (ignorado en git)
│   ├── turno-usuarios/
│   └── pacientes-practicas/
├── docker-compose.dev.yml               # Configuración de los contenedores Docker
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

En los archivos `application.yml` o `application.properties` de cada servicio, configura los datasources de la siguiente manera:

### En `ms-turno-usuarios`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/db_turno_usuarios
    username: postgres
    password: password123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # Las tablas ya fueron creadas por el script de init
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### En `ms-pacientes-practicas`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5434/db_pacientes_practicas
    username: postgres
    password: password123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # Las tablas ya fueron creadas por el script de init
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

---

## 🛠 Comandos Frecuentes de Gestión

| Acción | Comando |
| :--- | :--- |
| **Iniciar contenedores** | `docker compose -f docker-compose.dev.yml up -d` |
| **Detener contenedores (sin borrar datos)** | `docker compose -f docker-compose.dev.yml stop` |
| **Reanudar contenedores detenidos** | `docker compose -f docker-compose.dev.yml start` |
| **Reiniciar contenedores** | `docker compose -f docker-compose.dev.yml restart` |
| **Bajar contenedores** | `docker compose -f docker-compose.dev.yml down` |
| **Ver logs del servicio de turnos** | `docker compose -f docker-compose.dev.yml logs -f db-turno-usuarios` |
| **Ver logs del servicio de pacientes** | `docker compose -f docker-compose.dev.yml logs -f db-pacientes-practicas` |

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

### 1. `Error: bind: address already in use` (puerto ocupado)
- **Causa:** Tienes una instancia local de PostgreSQL u otro servicio corriendo en los puertos `5433` o `5434`.
- **Solución:**
  - Verifica qué proceso está usando el puerto:
    - En Windows: `netstat -ano | findstr :5433`
    - En Linux/Mac: `lsof -i :5433`
  - Puedes cerrar el servicio conflictivo o cambiar los puertos del host en `docker-compose.dev.yml` (por ejemplo, mapear `"5435:5432"`).

### 2. No se crearon las tablas o no se ejecutó el script `.sql`
- **Causa:** La carpeta `./data/turno-usuarios` o `./data/pacientes-practicas` ya tenía datos inicializados previamente.
- **Solución:** Sigue los pasos de [Reinicio Limpio / Reset de Datos](#-reinicio-limpio--reset-de-datos-importante).

### 3. Error `password authentication failed for user "postgres"`
- **Causa:** Existían datos previos con otra contraseña en la carpeta `./data/`.
- **Solución:** Elimina los datos existentes en `./data/` y vuelve a levantar el compose.

### 4. `docker: Cannot connect to the Docker daemon`
- **Causa:** Docker Desktop no está iniciado o el servicio de Docker está detenido.
- **Solución:** Inicia la aplicación Docker Desktop y espera a que el icono figure en verde ("Engine running").
