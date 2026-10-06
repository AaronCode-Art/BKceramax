# Ceramax — Backend

Backend de Ceramax, sistema de venta de ceramicos y acabados. API REST autenticada con JWT sobre PostgreSQL.

- Framework: Spring Boot 4.1.1 (Java 21)
- Seguridad: Spring Security + JWT (jjwt 0.12.6)
- Persistencia: Spring Data JPA + Hibernate 7.4.5
- BD: PostgreSQL (Neon, v18.6)
- Docs: Swagger UI (springdoc-openapi)
- Puerto: 8090

## Requisitos
- JDK 21
- Maven 3.9+ o Maven Wrapper (mvnw) incluido


## Dependencias

- Spring Boot 4.1.1
- Spring Web (`spring-boot-starter-web`)
- Spring Validation (`spring-boot-starter-validation`)
- Spring Data JPA (`spring-boot-starter-data-jpa`)
- PostgreSQL Driver (`org.postgresql:postgresql`)
- Spring Security (`spring-boot-starter-security`)
- JWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`, versión 0.12.6)
- Swagger / OpenAPI (`springdoc-openapi-starter-webmvc-ui`)
- Spring Actuator (`spring-boot-starter-actuator`)
- Lombok
- Cloudinary (`cloudinary-http44`)
- Spring Boot Test (`spring-boot-starter-test`)
- Spring Security Test (`spring-security-test`)


## Configuracion
Archivo: src/main/resources/application.properties
Valores sensibles por variables de entorno.

spring.datasource.url      -> host PostgreSQL
spring.datasource.username -> neondb_owner
spring.datasource.password -> ${DB_PASSWORD}
server.port                -> 8090
app.igv                    -> 0.18
app.jwt.secret             -> ${JWT_SECRET}
app.jwt.expiration-ms      -> 86400000
spring.jpa.hibernate.ddl-auto -> none (BD ya existe)

### Variables de entorno
DB_PASSWORD="tu_password_neon"
JWT_SECRET="clave_firma_tokens"

## Como ejecutar

### Opcion A - Maven Wrapper (recomendado)
cd backend-ceramax
export DB_PASSWORD="tu_password_neon"
mvnw.cmd spring-boot:run

### Opcion B - Compilar y ejecutar el JAR
cd backend-ceramax
export DB_PASSWORD="tu_password_neon"
mvnw.cmd clean package -DskipTests
java -jar target/ceramax-0.0.1-SNAPSHOT.jar

Al arrancar veras:
Tomcat started on port 8090
Started CeramaxApplication in X seconds

## Credenciales de prueba
| Rol | Email | Contrasena |
|---|---|---|
| Admin | admin@ceramax.com | admin123 |
| Vendedor | vendedor@ceramax.com | vendedor123 |
| Inactivo | inactivo@ceramax.com | inactivo123 |

Roles: admin y vendedor. Usuarios con activo=false no pueden iniciar sesion.

## Autenticacion (JWT)
1. Obtener token: POST /api/auth/login con email y password.
2. Enviar header en cada peticion protegida:
   Authorization: Bearer <token>

En Swagger UI (http://localhost:8090/swagger-ui.html) -> Authorize -> pegar "Bearer <token>".

### Ejemplo login
POST /api/auth/login
{ "email": "admin@ceramax.com", "password": "admin123" }

Respuesta:
{ "success": true, "data": { "token": "eyJ...", "tipo": "Bearer", "usuario": { "id": 1, "nombre": "Diana", "rol": "admin" } } }

## Nota sobre lazy loading
Las colecciones relacionadas (Producto.especificaciones, Venta.items, Cliente.direcciones) se cargan con @EntityGraph y los metodos de lectura usan @Transactional(readOnly = true). Esto evita el error "Cannot lazily initialize collection ... (no session)".

## Endpoints

### Autenticacion - /api/auth
POST /api/auth/login                Iniciar sesion y obtener JWT      [Publico]

### Catalogo - /api/catalogo
GET /api/catalogo/categorias        Lista de categorias               [Autenticado]
GET /api/catalogo/sucursales        Lista de sucursales               [Autenticado]

### Productos - /api/productos
GET /api/productos                  Lista productos (?q=)             [Autenticado]
GET /api/productos/{id}             Detalle de producto               [Autenticado]
GET /api/productos/destacados       Productos destacados              [Autenticado]
GET /api/productos/stock-bajo?umbral=10  Stock bajo                   [Autenticado]
POST /api/productos                 Crear producto                    [Autenticado]
PUT /api/productos/{id}             Actualizar producto               [Autenticado]
DELETE /api/productos/{id}          Eliminar producto                 [Autenticado]

### Clientes - /api/clientes
GET /api/clientes                   Lista clientes (con direcciones)  [Autenticado]
GET /api/clientes/{id}              Detalle de cliente                [Autenticado]
POST /api/clientes                  Crear cliente (varias dir)        [Autenticado]
PUT /api/clientes/{id}              Actualizar cliente                [Autenticado]
DELETE /api/clientes/{id}           Eliminar cliente                  [Autenticado]

### Ventas - /api/ventas
GET /api/ventas                     Ventas del usuario autenticado    [Autenticado]
GET /api/ventas/{id}                Detalle de venta                  [Autenticado]
POST /api/ventas                    Crear venta                       [Autenticado]
PATCH /api/ventas/{id}/estado       Cambiar estado                    [Autenticado]
DELETE /api/ventas/{id}             Eliminar venta                    [Autenticado]

Enums de Venta:
- Estado: en_proceso, listo_recojo, en_delivery, despachado, anulado
- Origen: panel, tienda_web
- Entrega: misma_persona, otra_persona, delivery
- TipoComprobante: BO, FAC

### Usuarios - /api/usuarios
GET /api/usuarios                   Lista de usuarios                 [Admin]
GET /api/usuarios/{id}              Detalle de usuario                [Admin]
POST /api/usuarios                  Crear usuario                     [Admin]
PUT /api/usuarios/{id}              Actualizar usuario                [Admin]
DELETE /api/usuarios/{id}           Eliminar usuario                  [Admin]

### Reportes - /api/reportes (Admin, fechas YYYY-MM-DD)
GET /api/reportes/resumen?desde=&hasta=
GET /api/reportes/ventas-por-dia?desde=&hasta=
GET /api/reportes/ventas-por-estado?desde=&hasta=
GET /api/reportes/top-productos?desde=&hasta=&limite=10
GET /api/reportes/ventas-por-vendedor?desde=&hasta=
GET /api/reportes/tienda-web

### Salud (publico)
GET /actuator/health                 Estado de la aplicacion


## Estructura del proyecto

backend-ceramax/
|- src/main/java/com/ceramax/ceramax/
|  |- CeramaxApplication.java      Punto de entrada
|  |- config/                      SecurityConfig, OpenApiConfig
|  |- controller/                  7 controllers REST
|  |- dto/                         Objetos request/response
|  |- exception/                   Manejo global de errores
|  |- mapper/                      Conversion entidad <-> DTO
|  |- model/                       11 entidades JPA
|  |- repository/                  9 repositorios
|  |- security/                    JWT, filtros, UserDetailsService
|  |- service/                     Logica de negocio (7 servicios)
|- src/main/resources/
|  |- application.properties       Configuracion
|- pom.xml                         Dependencias Maven
## Capas y responsabilidades
Controller -> recibe HTTP, valida y delega
Service    -> logica de negocio y transacciones
Repository -> acceso a datos (Spring Data JPA)
Mapper     -> mapea entidades a DTOs
Model      -> entidades JPA
DTO        -> objetos request/response
Security   -> emision y validacion de JWT
Config     -> seguridad y documentacion

## Entidades (model)
Usuario, Categoria, Sucursal, Producto, ProductoEspecificacion,
Cliente, ClienteDireccion, Venta, VentaItem, Carrito, CarritoItem

## Seguridad
- JwtService: genera y valida tokens JWT
- JwtFilter: intercepta peticiones, valida token y autentica
- CustomUserDetailsService: carga usuario por email
- SecurityConfig: reglas de acceso (publico/autenticado/ADMIN)

## Error handling
Todos los errores devuelven:
{ "success": false, "message": "...", "data": null, "timestamp": "..." }

GlobalExceptionHandler centraliza:
- RecursoNoEncontradoException -> 404
- ReglaNegocioException -> 400
- Errores de validacion (@Valid) -> 400
- Errores generales -> 500

## Swagger / OpenAPI
UI:  http://localhost:8090/swagger-ui.html
JSON: http://localhost:8090/v3/api-docs

## Pruebas rapidas con curl
1. Login
   curl -X POST http://localhost:8090/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@ceramax.com\",\"password\":\"admin123\"}"

2. Usar token
   curl http://localhost:8090/api/productos -H "Authorization: Bearer <TOKEN>"

3. Reporte resumen (admin)
   curl "http://localhost:8090/api/reportes/resumen?desde=2026-09-01&hasta=2026-09-03" -H "Authorization: Bearer <TOKEN>"

