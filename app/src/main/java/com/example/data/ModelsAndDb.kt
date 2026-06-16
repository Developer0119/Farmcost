package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// ==========================================
// ROOM ENTITIES (TABLES)
// ==========================================

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1, // Singleton profile
    val fullName: String,
    val mobileNumber: String,
    val village: String,
    val taluka: String,
    val district: String,
    val state: String,
    val preferredLanguage: String // "en", "hi", "mr"
)

@Entity(tableName = "farms")
data class Farm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val area: Double,
    val areaUnit: String, // "Acre", "Hectare"
    val village: String,
    val soilType: String,
    val irrigationType: String,
    val notes: String
)

@Entity(tableName = "crops")
data class Crop(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val variety: String,
    val season: String, // "Kharif", "Rabi", "Zaid", "Yearly"
    val farmId: Int, // Logical Foreign Key
    val plantingDate: Long,
    val expectedHarvestDate: Long
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val farmId: Int, // Logical Foreign Key
    val cropId: Int, // Logical Foreign Key
    val category: String, // Seeds, Fertilizers, Pesticides, Labor, etc.
    val amount: Double,
    val notes: String,
    val receiptPhotoPath: String? = null
)

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mobileNumber: String,
    val workType: String
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val workerId: Int, // Logical Foreign Key
    val wage: Double,
    val paidStatus: Boolean // true = Paid, false = Unpaid
)

@Entity(tableName = "income")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cropId: Int, // Logical Foreign Key
    val quantity: Double,
    val unit: String, // "kg", "Ton", "Quintal"
    val rate: Double,
    val buyerName: String,
    val saleDate: Long,
    val amount: Double // Calculated Quantity * Rate
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val preferredLanguage: String = "en",
    val darkMode: Boolean = false,
    val currencySymbol: String = "₹"
)

// ==========================================
// DATA ACCESS OBJECTS (DAOs)
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = 1")
    fun getUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms ORDER BY id DESC")
    fun getAllFarms(): Flow<List<Farm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarm(farm: Farm)

    @Update
    suspend fun updateFarm(farm: Farm)

    @Query("DELETE FROM farms WHERE id = :id")
    suspend fun deleteFarmById(id: Int)
}

@Dao
interface CropDao {
    @Query("SELECT * FROM crops ORDER BY id DESC")
    fun getAllCrops(): Flow<List<Crop>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: Crop)

    @Update
    suspend fun updateCrop(crop: Crop)

    @Query("DELETE FROM crops WHERE id = :id")
    suspend fun deleteCropById(id: Int)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)
}

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY id DESC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorkerById(id: Int)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: Int)
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income ORDER BY saleDate DESC")
    fun getAllIncome(): Flow<List<Income>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income)

    @Update
    suspend fun updateIncome(income: Income)

    @Query("DELETE FROM income WHERE id = :id")
    suspend fun deleteIncomeById(id: Int)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}

// ==========================================
// DATABASE DEF
// ==========================================

@Database(
    entities = [
        User::class, Farm::class, Crop::class, Expense::class,
        Worker::class, Attendance::class, Income::class, AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun farmDao(): FarmDao
    abstract fun cropDao(): CropDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun incomeDao(): IncomeDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "farm_cost_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// REPOSITORY IMPLEMENTATION
// ==========================================

class FarmCostRepository(private val db: AppDatabase) {
    val userFlow: Flow<User?> = db.userDao().getUser()
    val farmsFlow: Flow<List<Farm>> = db.farmDao().getAllFarms()
    val cropsFlow: Flow<List<Crop>> = db.cropDao().getAllCrops()
    val expensesFlow: Flow<List<Expense>> = db.expenseDao().getAllExpenses()
    val workersFlow: Flow<List<Worker>> = db.workerDao().getAllWorkers()
    val attendanceFlow: Flow<List<Attendance>> = db.attendanceDao().getAllAttendance()
    val incomeFlow: Flow<List<Income>> = db.incomeDao().getAllIncome()
    val settingsFlow: Flow<AppSettings?> = db.settingsDao().getSettings()

    // SUSPEND DB MUTATORS
    suspend fun insertUser(user: User) = db.userDao().insertUser(user)
    suspend fun deleteUser(user: User) = db.userDao().deleteUser(user)

    suspend fun insertFarm(farm: Farm) = db.farmDao().insertFarm(farm)
    suspend fun updateFarm(farm: Farm) = db.farmDao().updateFarm(farm)
    suspend fun deleteFarm(farmId: Int) = db.farmDao().deleteFarmById(farmId)

    suspend fun insertCrop(crop: Crop) = db.cropDao().insertCrop(crop)
    suspend fun updateCrop(crop: Crop) = db.cropDao().updateCrop(crop)
    suspend fun deleteCrop(cropId: Int) = db.cropDao().deleteCropById(cropId)

    suspend fun insertExpense(expense: Expense) = db.expenseDao().insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = db.expenseDao().updateExpense(expense)
    suspend fun deleteExpense(expenseId: Int) = db.expenseDao().deleteExpenseById(expenseId)

    suspend fun insertWorker(worker: Worker) = db.workerDao().insertWorker(worker)
    suspend fun updateWorker(worker: Worker) = db.workerDao().updateWorker(worker)
    suspend fun deleteWorker(workerId: Int) = db.workerDao().deleteWorkerById(workerId)

    suspend fun insertAttendance(attendance: Attendance) = db.attendanceDao().insertAttendance(attendance)
    suspend fun updateAttendance(attendance: Attendance) = db.attendanceDao().updateAttendance(attendance)
    suspend fun deleteAttendance(attendanceId: Int) = db.attendanceDao().deleteAttendanceById(attendanceId)

    suspend fun insertIncome(income: Income) = db.incomeDao().insertIncome(income)
    suspend fun updateIncome(income: Income) = db.incomeDao().updateIncome(income)
    suspend fun deleteIncome(incomeId: Int) = db.incomeDao().deleteIncomeById(incomeId)

    suspend fun insertSettings(settings: AppSettings) = db.settingsDao().insertSettings(settings)

    // BACKUP & RESTORE UTILITIES
    suspend fun generateBackupJsonString(): String {
        val root = JSONObject()

        // User
        val userArr = JSONArray()
        db.userDao().getUser().collectFirst()?.let { u ->
            val uObj = JSONObject().apply {
                put("id", u.id)
                put("fullName", u.fullName)
                put("mobileNumber", u.mobileNumber)
                put("village", u.village)
                put("taluka", u.taluka)
                put("district", u.district)
                put("state", u.state)
                put("preferredLanguage", u.preferredLanguage)
            }
            userArr.put(uObj)
        }
        root.put("users", userArr)

        // Farms
        val farmArr = JSONArray()
        db.farmDao().getAllFarms().collectFirst()?.forEach { f ->
            val fObj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("area", f.area)
                put("areaUnit", f.areaUnit)
                put("village", f.village)
                put("soilType", f.soilType)
                put("irrigationType", f.irrigationType)
                put("notes", f.notes)
            }
            farmArr.put(fObj)
        }
        root.put("farms", farmArr)

        // Crops
        val cropArr = JSONArray()
        db.cropDao().getAllCrops().collectFirst()?.forEach { c ->
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("variety", c.variety)
                put("season", c.season)
                put("farmId", c.farmId)
                put("plantingDate", c.plantingDate)
                put("expectedHarvestDate", c.expectedHarvestDate)
            }
            cropArr.put(cObj)
        }
        root.put("crops", cropArr)

        // Expenses
        val expenseArr = JSONArray()
        db.expenseDao().getAllExpenses().collectFirst()?.forEach { e ->
            val eObj = JSONObject().apply {
                put("id", e.id)
                put("date", e.date)
                put("farmId", e.farmId)
                put("cropId", e.cropId)
                put("category", e.category)
                put("amount", e.amount)
                put("notes", e.notes)
                put("receiptPhotoPath", e.receiptPhotoPath ?: "")
            }
            expenseArr.put(eObj)
        }
        root.put("expenses", expenseArr)

        // Workers
        val workerArr = JSONArray()
        db.workerDao().getAllWorkers().collectFirst()?.forEach { w ->
            val wObj = JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("mobileNumber", w.mobileNumber)
                put("workType", w.workType)
            }
            workerArr.put(wObj)
        }
        root.put("workers", workerArr)

        // Attendance
        val attendanceArr = JSONArray()
        db.attendanceDao().getAllAttendance().collectFirst()?.forEach { a ->
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("date", a.date)
                put("workerId", a.workerId)
                put("wage", a.wage)
                put("paidStatus", a.paidStatus)
            }
            attendanceArr.put(aObj)
        }
        root.put("attendance", attendanceArr)

        // Income
        val incomeArr = JSONArray()
        db.incomeDao().getAllIncome().collectFirst()?.forEach { inc ->
            val iObj = JSONObject().apply {
                put("id", inc.id)
                put("cropId", inc.cropId)
                put("quantity", inc.quantity)
                put("unit", inc.unit)
                put("rate", inc.rate)
                put("buyerName", inc.buyerName)
                put("saleDate", inc.saleDate)
                put("amount", inc.amount)
            }
            incomeArr.put(iObj)
        }
        root.put("income", incomeArr)

        // Settings
        val settingsArr = JSONArray()
        db.settingsDao().getSettings().collectFirst()?.let { s ->
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("preferredLanguage", s.preferredLanguage)
                put("darkMode", s.darkMode)
                put("currencySymbol", s.currencySymbol)
            }
            settingsArr.put(sObj)
        }
        root.put("settings", settingsArr)

        return root.toString(2)
    }

    suspend fun restoreFromJsonString(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)

            // Dynamic clearing is safer to prevent conflict/duplicate PKs on restore.
            // Let's clear database tables before restoration.
            db.clearAllTables()

            // 1. User
            if (root.has("users")) {
                val arr = root.getJSONArray("users")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertUser(
                        User(
                            id = oo.optInt("id", 1),
                            fullName = oo.optString("fullName", "Farmer"),
                            mobileNumber = oo.optString("mobileNumber", ""),
                            village = oo.optString("village", ""),
                            taluka = oo.optString("taluka", ""),
                            district = oo.optString("district", ""),
                            state = oo.optString("state", ""),
                            preferredLanguage = oo.optString("preferredLanguage", "en")
                        )
                    )
                }
            }

            // 2. Farms
            if (root.has("farms")) {
                val arr = root.getJSONArray("farms")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertFarm(
                        Farm(
                            id = oo.optInt("id", 0),
                            name = oo.optString("name", ""),
                            area = oo.optDouble("area", 0.0),
                            areaUnit = oo.optString("areaUnit", "Acre"),
                            village = oo.optString("village", ""),
                            soilType = oo.optString("soilType", "Black Soil"),
                            irrigationType = oo.optString("irrigationType", "Drip Irrigation"),
                            notes = oo.optString("notes", "")
                        )
                    )
                }
            }

            // 3. Crops
            if (root.has("crops")) {
                val arr = root.getJSONArray("crops")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertCrop(
                        Crop(
                            id = oo.optInt("id", 0),
                            name = oo.optString("name", ""),
                            variety = oo.optString("variety", ""),
                            season = oo.optString("season", "Kharif"),
                            farmId = oo.optInt("farmId", 0),
                            plantingDate = oo.optLong("plantingDate", System.currentTimeMillis()),
                            expectedHarvestDate = oo.optLong("expectedHarvestDate", System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000)
                        )
                    )
                }
            }

            // 4. Expenses
            if (root.has("expenses")) {
                val arr = root.getJSONArray("expenses")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertExpense(
                        Expense(
                            id = oo.optInt("id", 0),
                            date = oo.optLong("date", System.currentTimeMillis()),
                            farmId = oo.optInt("farmId", 0),
                            cropId = oo.optInt("cropId", 0),
                            category = oo.optString("category", "Seeds"),
                            amount = oo.optDouble("amount", 0.0),
                            notes = oo.optString("notes", ""),
                            receiptPhotoPath = oo.optString("receiptPhotoPath", "").let { if (it.isEmpty()) null else it }
                        )
                    )
                }
            }

            // 5. Workers
            if (root.has("workers")) {
                val arr = root.getJSONArray("workers")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertWorker(
                        Worker(
                            id = oo.optInt("id", 0),
                            name = oo.optString("name", ""),
                            mobileNumber = oo.optString("mobileNumber", ""),
                            workType = oo.optString("workType", "General Labor")
                        )
                    )
                }
            }

            // 6. Attendance
            if (root.has("attendance")) {
                val arr = root.getJSONArray("attendance")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertAttendance(
                        Attendance(
                            id = oo.optInt("id", 0),
                            date = oo.optLong("date", System.currentTimeMillis()),
                            workerId = oo.optInt("workerId", 0),
                            wage = oo.optDouble("wage", 0.0),
                            paidStatus = oo.optBoolean("paidStatus", false)
                        )
                    )
                }
            }

            // 7. Income
            if (root.has("income")) {
                val arr = root.getJSONArray("income")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertIncome(
                        Income(
                            id = oo.optInt("id", 0),
                            cropId = oo.optInt("cropId", 0),
                            quantity = oo.optDouble("quantity", 0.0),
                            unit = oo.optString("unit", "kg"),
                            rate = oo.optDouble("rate", 0.0),
                            buyerName = oo.optString("buyerName", ""),
                            saleDate = oo.optLong("saleDate", System.currentTimeMillis()),
                            amount = oo.optDouble("amount", 0.0)
                        )
                    )
                }
            }

            // 8. Settings
            if (root.has("settings")) {
                val arr = root.getJSONArray("settings")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    insertSettings(
                        AppSettings(
                            id = oo.optInt("id", 1),
                            preferredLanguage = oo.optString("preferredLanguage", "en"),
                            darkMode = oo.optBoolean("darkMode", false),
                            currencySymbol = oo.optString("currencySymbol", "₹")
                        )
                    )
                }
            }

            true
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    // Helper to get raw snapshot of flow once
    private suspend fun <T> Flow<T>.collectFirst(): T? {
        return this.firstOrNull()
    }
}
