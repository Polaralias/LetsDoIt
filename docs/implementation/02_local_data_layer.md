# Implementation Phase 2: Local Data Layer

This document details the implementation of the local database using Room.

## Goal
Implement the persistent local storage for Tasks, Lists, Folders, and Spaces using Room, ensuring proper relationships and dependency injection.

## Prompts

### 1. Define Entities
> Create data classes annotated with `@Entity` in `data/local/entity/`:
>
> **SpaceEntity**
> *   `id`: String (Primary Key)
> *   `name`: String
> *   `createdAt`: Long
> *   `updatedAt`: Long
>
> **FolderEntity**
> *   `id`: String (Primary Key)
> *   `spaceId`: String (Foreign Key to SpaceEntity)
> *   `name`: String
> *   `orderIndex`: Int
>
> **ListEntity**
> *   `id`: String (Primary Key)
> *   `folderId`: String? (Foreign Key to FolderEntity, nullable as lists can exist in spaces directly or be folderless)
> *   `spaceId`: String (Foreign Key to SpaceEntity)
> *   `name`: String
> *   `color`: String
>
> **TaskEntity**
> *   `id`: String (Primary Key)
> *   `listId`: String (Foreign Key to ListEntity)
> *   `title`: String
> *   `description`: String?
> *   `status`: String
> *   `dueDate`: Long?
> *   `priority`: Int
> *   `createdAt`: Long
> *   `updatedAt`: Long
> *   `isSynced`: Boolean (default true, set false on local change)

### 2. Create Type Converters
> Create `data/local/converters/DateConverter.kt` to convert `Long` timestamps to/from `Date` or `LocalDateTime` objects if you choose to use those types in entities (though keeping Long in entities is often simpler).
>
> *Action*: Annotate the Database class with `@TypeConverters`.

### 3. Create DAOs
> Create interfaces annotated with `@Dao` in `data/local/dao/`:
>
> **SpaceDao**, **FolderDao**, **ListDao**
> *   Insert (OnConflictStrategy.REPLACE)
> *   Update
> *   Delete
> *   GetById
> *   GetAll (Flow<List<Entity>>)
>
> **TaskDao**
> *   Insert, Update, Delete
> *   `getTaskById(id: String): Flow<TaskEntity?>`
> *   `getTasksByListId(listId: String): Flow<List<TaskEntity>>`
> *   `getAllTasks(): Flow<List<TaskEntity>>`
> *   `getUnsyncedTasks(): List<TaskEntity>` (for sync worker)

### 4. Create Database Class
> Create `AppDatabase` abstract class in `data/local/`:
> *   Extend `RoomDatabase`.
> *   Define abstract functions returning each Dao.
> *   Annotate with `@Database(entities = [...], version = 1)`.

### 5. Hilt Module for Database
> Create `core/di/DatabaseModule.kt`:
> *   Annotate with `@Module` and `@InstallIn(SingletonComponent::class)`.
> *   Provide `AppDatabase` (Singleton).
> *   Provide each Dao (SpaceDao, FolderDao, ListDao, TaskDao) from the database instance.

### 6. Verification
> Write an instrumentation test in `androidTest/java/com.polaralias.letsdoit/data/local/TaskDaoTest.kt`:
> *   Initialize an in-memory Room database.
> *   Insert a Task.
> *   Retrieve the Task and verify its properties match.
