# 📂 Gestor de Archivos en Java

Este proyecto es una aplicación de consola robusta desarrollada en **Java** diseñada para la gestión y administración de sistemas de archivos locales. Permite realizar operaciones CRUD (Crear, Leer, Actualizar, Borrar) sobre archivos y directorios de forma interactiva.

---

## 🚀 Funcionalidades Detalladas

El sistema ofrece una interfaz de comandos que permite interactuar con el almacenamiento del equipo mediante las siguientes funciones:

### 1. Exploración de Directorios
* **Listar Contenido:** Muestra todos los archivos y carpetas dentro de una ruta específica, detallando si se trata de un archivo o un directorio.
* **Ruta Actual:** El sistema mantiene el rastro de la ubicación actual del usuario dentro del árbol de directorios.

### 2. Manipulación de Archivos y Carpetas
* **Creación de Directorios:** Permite generar nuevas carpetas (`mkdir`) para organizar la información.
* **Creación de Archivos:** Genera archivos vacíos de cualquier extensión dentro del directorio seleccionado.
* **Renombrado Dinámico:** Cambia el nombre de cualquier elemento existente validando que el nuevo nombre sea válido.

### 3. Gestión de Seguridad y Borrado
* **Eliminación de Archivos:** Borrado permanente de archivos individuales.
* **Eliminación de Directorios:** Capacidad para eliminar carpetas (el sistema incluye lógica para manejar si la carpeta contiene elementos).

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Java 8+
* **Librerías Estándar:** * `java.io.File`: Para la manipulación física de archivos.
    * `java.util.Scanner`: Para la lectura de comandos del usuario.

---

## 📂 Estructura del Código

* **`Main.java`**: Actúa como el controlador principal. Gestiona el bucle de la aplicación y el menú de usuario.
* **`Gestor.java`**: Contiene la lógica de negocio. Es la clase encargada de invocar los métodos del sistema operativo para manipular los archivos, separando la interfaz de la lógica.

---

## 💻 Instalación y Uso

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/gael-marquez/GestorArchivos.git](https://github.com/gael-marquez/GestorArchivos.git)

2. **Compilar el proyecto:**
```bash
javac *.java

3. **Ejecutar:
```bash
java Main
## 👤 Autor
Desarrollado por Gael Márquez y Brandon Trejo.
