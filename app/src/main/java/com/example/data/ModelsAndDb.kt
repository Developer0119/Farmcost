package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// ==========================================
// DATA MODELS (PLAIN DATA CLASSES - NO SQLITE)
// ==========================================

data class User(
    val id: Int = 1, // Singleton profile
    val fullName: String,
    val mobileNumber: String,
    val village: String,
    val taluka: String,
    val district: String,
    val state: String,
    val preferredLanguage: String // "en", "hi", "mr"
)

data class Farm(
    val id: Int = 0,
    val name: String,
    val area: Double,
    val areaUnit: String, // "Acre", "Hectare"
    val village: String,
    val soilType: String,
    val irrigationType: String,
    val notes: String
)

data class Crop(
    val id: Int = 0,
    val name: String,
    val variety: String,
    val season: String, // "Kharif", "Rabi", "Zaid", "Yearly"
    val farmId: Int, // Logical Foreign Key
    val plantingDate: Long,
    val expectedHarvestDate: Long
)

data class Expense(
    val id: Int = 0,
    val date: Long,
    val farmId: Int, // Logical Foreign Key
    val cropId: Int, // Logical Foreign Key
    val category: String, // Seeds, Fertilizers, Pesticides, Labor, etc.
    val amount: Double,
    val notes: String,
    val receiptPhotoPath: String? = null
)

data class Worker(
    val id: Int = 0,
    val name: String,
    val mobileNumber: String,
    val workType: String
)

data class Attendance(
    val id: Int = 0,
    val date: Long,
    val workerId: Int, // Logical Foreign Key
    val wage: Double,
    val paidStatus: Boolean // true = Paid, false = Unpaid
)

data class Income(
    val id: Int = 0,
    val cropId: Int, // Logical Foreign Key
    val quantity: Double,
    val unit: String, // "kg", "Ton", "Quintal"
    val rate: Double,
    val buyerName: String,
    val saleDate: Long,
    val amount: Double // Calculated Quantity * Rate
)

data class AppSettings(
    val id: Int = 1,
    val preferredLanguage: String = "en",
    val darkMode: Boolean = false,
    val currencySymbol: String = "₹"
)

// ==========================================
// REPOSITORY IMPLEMENTATION (PHYSICAL FILES)
// ==========================================

class FarmCostRepository(private val context: Context) {
    
    // We will store all files in "Android/data/<package>/files/FarmCost AI"
    // This organizes separate physical JSON files inside the app's standard directory.
    private val dataFolder: File by lazy {
        val folder = File(context.getExternalFilesDir(null), "FarmCost AI")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        folder
    }

    private val userFile by lazy { File(dataFolder, "user.json") }
    private val farmsFile by lazy { File(dataFolder, "farms.json") }
    private val cropsFile by lazy { File(dataFolder, "crops.json") }
    private val expensesFile by lazy { File(dataFolder, "expenses.json") }
    private val workersFile by lazy { File(dataFolder, "workers.json") }
    private val attendanceFile by lazy { File(dataFolder, "attendance.json") }
    private val incomeFile by lazy { File(dataFolder, "income.json") }
    private val settingsFile by lazy { File(dataFolder, "settings.json") }

    private val _userState = MutableStateFlow<User?>(null)
    val userFlow: Flow<User?> = _userState.asStateFlow()

    private val _farmsState = MutableStateFlow<List<Farm>>(emptyList())
    val farmsFlow: Flow<List<Farm>> = _farmsState.asStateFlow()

    private val _cropsState = MutableStateFlow<List<Crop>>(emptyList())
    val cropsFlow: Flow<List<Crop>> = _cropsState.asStateFlow()

    private val _expensesState = MutableStateFlow<List<Expense>>(emptyList())
    val expensesFlow: Flow<List<Expense>> = _expensesState.asStateFlow()

    private val _workersState = MutableStateFlow<List<Worker>>(emptyList())
    val workersFlow: Flow<List<Worker>> = _workersState.asStateFlow()

    private val _attendanceState = MutableStateFlow<List<Attendance>>(emptyList())
    val attendanceFlow: Flow<List<Attendance>> = _attendanceState.asStateFlow()

    private val _incomeState = MutableStateFlow<List<Income>>(emptyList())
    val incomeFlow: Flow<List<Income>> = _incomeState.asStateFlow()

    private val _settingsState = MutableStateFlow<AppSettings?>(null)
    val settingsFlow: Flow<AppSettings?> = _settingsState.asStateFlow()

    init {
        loadUser()
        loadFarms()
        loadCrops()
        loadExpenses()
        loadWorkers()
        loadAttendance()
        loadIncome()
        loadSettings()
    }

    private fun loadUser() {
        try {
            if (userFile.exists()) {
                val s = userFile.readText()
                if (s.isNotBlank()) {
                    val json = JSONObject(s)
                    _userState.value = User(
                        id = json.optInt("id", 1),
                        fullName = json.optString("fullName"),
                        mobileNumber = json.optString("mobileNumber"),
                        village = json.optString("village"),
                        taluka = json.optString("taluka"),
                        district = json.optString("district"),
                        state = json.optString("state"),
                        preferredLanguage = json.optString("preferredLanguage")
                    )
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _userState.value = null
    }

    private fun saveUser(u: User?) {
        try {
            _userState.value = u
            if (u == null) {
                if (userFile.exists()) userFile.delete()
            } else {
                val json = JSONObject().apply {
                    put("id", u.id)
                    put("fullName", u.fullName)
                    put("mobileNumber", u.mobileNumber)
                    put("village", u.village)
                    put("taluka", u.taluka)
                    put("district", u.district)
                    put("state", u.state)
                    put("preferredLanguage", u.preferredLanguage)
                }
                userFile.writeText(json.toString(4))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFarms() {
        val list = mutableListOf<Farm>()
        try {
            if (farmsFile.exists()) {
                val s = farmsFile.readText()
                if (s.isNotBlank()) {
                    val arr = JSONArray(s)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Farm(
                                id = obj.optInt("id"),
                                name = obj.optString("name"),
                                area = obj.optDouble("area"),
                                areaUnit = obj.optString("areaUnit"),
                                village = obj.optString("village"),
                                soilType = obj.optString("soilType"),
                                irrigationType = obj.optString("irrigationType"),
                                notes = obj.optString("notes")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _farmsState.value = list
    }

    private fun saveFarms(list: List<Farm>) {
        try {
            _farmsState.value = list
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("area", item.area)
                    put("areaUnit", item.areaUnit)
                    put("village", item.village)
                    put("soilType", item.soilType)
                    put("irrigationType", item.irrigationType)
                    put("notes", item.notes)
                }
                arr.put(obj)
            }
            farmsFile.writeText(arr.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCrops() {
        val list = mutableListOf<Crop>()
        try {
            if (cropsFile.exists()) {
                val s = cropsFile.readText()
                if (s.isNotBlank()) {
                    val arr = JSONArray(s)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Crop(
                                id = obj.optInt("id"),
                                name = obj.optString("name"),
                                variety = obj.optString("variety"),
                                season = obj.optString("season"),
                                farmId = obj.optInt("farmId"),
                                plantingDate = obj.optLong("plantingDate"),
                                expectedHarvestDate = obj.optLong("expectedHarvestDate")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _cropsState.value = list
    }

    private fun saveCrops(list: List<Crop>) {
        try {
            _cropsState.value = list
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("variety", item.variety)
                    put("season", item.season)
                    put("farmId", item.farmId)
                    put("plantingDate", item.plantingDate)
                    put("expectedHarvestDate", item.expectedHarvestDate)
                }
                arr.put(obj)
            }
            cropsFile.writeText(arr.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadExpenses() {
        val list = mutableListOf<Expense>()
        try {
            if (expensesFile.exists()) {
                val s = expensesFile.readText()
                if (s.isNotBlank()) {
                    val arr = JSONArray(s)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Expense(
                                id = obj.optInt("id"),
                                date = obj.optLong("date"),
                                farmId = obj.optInt("farmId"),
                                cropId = obj.optInt("cropId"),
                                category = obj.optString("category"),
                                amount = obj.optDouble("amount"),
                                notes = obj.optString("notes"),
                                receiptPhotoPath = if (obj.isNull("receiptPhotoPath") || obj.optString("receiptPhotoPath").isEmpty()) null else obj.optString("receiptPhotoPath")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _expensesState.value = list
    }

    private fun saveExpenses(list: List<Expense>) {
        try {
            _expensesState.value = list
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("date", item.date)
                    put("farmId", item.farmId)
                    put("cropId", item.cropId)
                    put("category", item.category)
                    put("amount", item.amount)
                    put("notes", item.notes)
                    put("receiptPhotoPath", item.receiptPhotoPath ?: "")
                }
                arr.put(obj)
            }
            expensesFile.writeText(arr.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadWorkers() {
        val list = mutableListOf<Worker>()
        try {
            if (workersFile.exists()) {
                val s = workersFile.readText()
                if (s.isNotBlank()) {
                    val arr = JSONArray(s)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Worker(
                                id = obj.optInt("id"),
                                name = obj.optString("name"),
                                mobileNumber = obj.optString("mobileNumber"),
                                workType = obj.optString("workType")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _workersState.value = list
    }

    private fun saveWorkers(list: List<Worker>) {
        try {
            _workersState.value = list
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("mobileNumber", item.mobileNumber)
                    put("workType", item.workType)
                }
                arr.put(obj)
            }
            workersFile.writeText(arr.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAttendance() {
        val list = mutableListOf<Attendance>()
        try {
            if (attendanceFile.exists()) {
                val s = attendanceFile.readText()
                if (s.isNotBlank()) {
                    val arr = JSONArray(s)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Attendance(
                                id = obj.optInt("id"),
                                date = obj.optLong("date"),
                                workerId = obj.optInt("workerId"),
                                wage = obj.optDouble("wage"),
                                paidStatus = obj.optBoolean("paidStatus")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _attendanceState.value = list
    }

    private fun saveAttendance(list: List<Attendance>) {
        try {
            _attendanceState.value = list
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("date", item.date)
                    put("workerId", item.workerId)
                    put("wage", item.wage)
                    put("paidStatus", item.paidStatus)
                }
                arr.put(obj)
            }
            attendanceFile.writeText(arr.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadIncome() {
        val list = mutableListOf<Income>()
        try {
            if (incomeFile.exists()) {
                val s = incomeFile.readText()
                if (s.isNotBlank()) {
                    val arr = JSONArray(s)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Income(
                                id = obj.optInt("id"),
                                cropId = obj.optInt("cropId"),
                                quantity = obj.optDouble("quantity"),
                                unit = obj.optString("unit"),
                                rate = obj.optDouble("rate"),
                                buyerName = obj.optString("buyerName"),
                                saleDate = obj.optLong("saleDate"),
                                amount = obj.optDouble("amount")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _incomeState.value = list
    }

    private fun saveIncome(list: List<Income>) {
        try {
            _incomeState.value = list
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("cropId", item.cropId)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("rate", item.rate)
                    put("buyerName", item.buyerName)
                    put("saleDate", item.saleDate)
                    put("amount", item.amount)
                }
                arr.put(obj)
            }
            incomeFile.writeText(arr.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                val s = settingsFile.readText()
                if (s.isNotBlank()) {
                    val obj = JSONObject(s)
                    _settingsState.value = AppSettings(
                        id = obj.optInt("id", 1),
                        preferredLanguage = obj.optString("preferredLanguage", "en"),
                        darkMode = obj.optBoolean("darkMode", false),
                        currencySymbol = obj.optString("currencySymbol", "₹")
                    )
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _settingsState.value = AppSettings()
    }

    private fun saveSettings(sObj: AppSettings?) {
        try {
            _settingsState.value = sObj ?: AppSettings()
            if (sObj == null) {
                if (settingsFile.exists()) settingsFile.delete()
            } else {
                val obj = JSONObject().apply {
                    put("id", sObj.id)
                    put("preferredLanguage", sObj.preferredLanguage)
                    put("darkMode", sObj.darkMode)
                    put("currencySymbol", sObj.currencySymbol)
                }
                settingsFile.writeText(obj.toString(4))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertUser(user: User) {
        saveUser(user)
    }

    suspend fun deleteUser(user: User) {
        saveUser(null)
    }

    suspend fun insertFarm(farm: Farm) {
        val list = _farmsState.value.toMutableList()
        val newId = if (farm.id == 0) {
            if (list.isEmpty()) 1 else (list.maxOf { it.id } + 1)
        } else {
            farm.id
        }
        list.add(farm.copy(id = newId))
        saveFarms(list)
    }

    suspend fun updateFarm(farm: Farm) {
        val list = _farmsState.value.map { if (it.id == farm.id) farm else it }
        saveFarms(list)
    }

    suspend fun deleteFarm(farmId: Int) {
        val list = _farmsState.value.filter { it.id != farmId }
        saveFarms(list)
    }

    suspend fun insertCrop(crop: Crop) {
        val list = _cropsState.value.toMutableList()
        val newId = if (crop.id == 0) {
            if (list.isEmpty()) 1 else (list.maxOf { it.id } + 1)
        } else {
            crop.id
        }
        list.add(crop.copy(id = newId))
        saveCrops(list)
    }

    suspend fun updateCrop(crop: Crop) {
        val list = _cropsState.value.map { if (it.id == crop.id) crop else it }
        saveCrops(list)
    }

    suspend fun deleteCrop(cropId: Int) {
        val list = _cropsState.value.filter { it.id != cropId }
        saveCrops(list)
    }

    suspend fun insertExpense(expense: Expense) {
        val list = _expensesState.value.toMutableList()
        val newId = if (expense.id == 0) {
            if (list.isEmpty()) 1 else (list.maxOf { it.id } + 1)
        } else {
            expense.id
        }
        list.add(expense.copy(id = newId))
        saveExpenses(list)
    }

    suspend fun updateExpense(expense: Expense) {
        val list = _expensesState.value.map { if (it.id == expense.id) expense else it }
        saveExpenses(list)
    }

    suspend fun deleteExpense(expenseId: Int) {
        val list = _expensesState.value.filter { it.id != expenseId }
        saveExpenses(list)
    }

    suspend fun insertWorker(worker: Worker) {
        val list = _workersState.value.toMutableList()
        val newId = if (worker.id == 0) {
            if (list.isEmpty()) 1 else (list.maxOf { it.id } + 1)
        } else {
            worker.id
        }
        list.add(worker.copy(id = newId))
        saveWorkers(list)
    }

    suspend fun updateWorker(worker: Worker) {
        val list = _workersState.value.map { if (it.id == worker.id) worker else it }
        saveWorkers(list)
    }

    suspend fun deleteWorker(workerId: Int) {
        val list = _workersState.value.filter { it.id != workerId }
        saveWorkers(list)
    }

    suspend fun insertAttendance(attendance: Attendance) {
        val list = _attendanceState.value.toMutableList()
        val newId = if (attendance.id == 0) {
            if (list.isEmpty()) 1 else (list.maxOf { it.id } + 1)
        } else {
            attendance.id
        }
        list.add(attendance.copy(id = newId))
        saveAttendance(list)
    }

    suspend fun updateAttendance(attendance: Attendance) {
        val list = _attendanceState.value.map { if (it.id == attendance.id) attendance else it }
        saveAttendance(list)
    }

    suspend fun deleteAttendance(attendanceId: Int) {
        val list = _attendanceState.value.filter { it.id != attendanceId }
        saveAttendance(list)
    }

    suspend fun insertIncome(income: Income) {
        val list = _incomeState.value.toMutableList()
        val newId = if (income.id == 0) {
            if (list.isEmpty()) 1 else (list.maxOf { it.id } + 1)
        } else {
            income.id
        }
        list.add(income.copy(id = newId))
        saveIncome(list)
    }

    suspend fun updateIncome(income: Income) {
        val list = _incomeState.value.map { if (it.id == income.id) income else it }
        saveIncome(list)
    }

    suspend fun deleteIncome(incomeId: Int) {
        val list = _incomeState.value.filter { it.id != incomeId }
        saveIncome(list)
    }

    suspend fun insertSettings(settings: AppSettings) {
        saveSettings(settings)
    }

    suspend fun generateBackupJsonString(): String {
        val root = JSONObject()

        // User
        val u = _userState.value
        val userArr = JSONArray()
        if (u != null) {
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
        _farmsState.value.forEach { f ->
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
        _cropsState.value.forEach { c ->
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
        _expensesState.value.forEach { e ->
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
        _workersState.value.forEach { w ->
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
        _attendanceState.value.forEach { a ->
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
        _incomeState.value.forEach { inc ->
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
        val s = _settingsState.value
        val settingsArr = JSONArray()
        if (s != null) {
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

            // Reset memory states
            _userState.value = null
            _farmsState.value = emptyList()
            _cropsState.value = emptyList()
            _expensesState.value = emptyList()
            _workersState.value = emptyList()
            _attendanceState.value = emptyList()
            _incomeState.value = emptyList()
            _settingsState.value = null

            // 1. User
            if (root.has("users")) {
                val arr = root.getJSONArray("users")
                if (arr.length() > 0) {
                    val oo = arr.getJSONObject(0)
                    val u = User(
                        id = oo.optInt("id", 1),
                        fullName = oo.optString("fullName", "Farmer"),
                        mobileNumber = oo.optString("mobileNumber", ""),
                        village = oo.optString("village", ""),
                        taluka = oo.optString("taluka", ""),
                        district = oo.optString("district", ""),
                        state = oo.optString("state", ""),
                        preferredLanguage = oo.optString("preferredLanguage", "en")
                    )
                    saveUser(u)
                }
            }

            // 2. Farms
            val farmsList = mutableListOf<Farm>()
            if (root.has("farms")) {
                val arr = root.getJSONArray("farms")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    farmsList.add(
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
            saveFarms(farmsList)

            // 3. Crops
            val cropsList = mutableListOf<Crop>()
            if (root.has("crops")) {
                val arr = root.getJSONArray("crops")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    cropsList.add(
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
            saveCrops(cropsList)

            // 4. Expenses
            val expensesList = mutableListOf<Expense>()
            if (root.has("expenses")) {
                val arr = root.getJSONArray("expenses")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    expensesList.add(
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
            saveExpenses(expensesList)

            // 5. Workers
            val workersList = mutableListOf<Worker>()
            if (root.has("workers")) {
                val arr = root.getJSONArray("workers")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    workersList.add(
                        Worker(
                            id = oo.optInt("id", 0),
                            name = oo.optString("name", ""),
                            mobileNumber = oo.optString("mobileNumber", ""),
                            workType = oo.optString("workType", "General Labor")
                        )
                    )
                }
            }
            saveWorkers(workersList)

            // 6. Attendance
            val attendanceList = mutableListOf<Attendance>()
            if (root.has("attendance")) {
                val arr = root.getJSONArray("attendance")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    attendanceList.add(
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
            saveAttendance(attendanceList)

            // 7. Income
            val incomeList = mutableListOf<Income>()
            if (root.has("income")) {
                val arr = root.getJSONArray("income")
                for (i in 0 until arr.length()) {
                    val oo = arr.getJSONObject(i)
                    incomeList.add(
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
            saveIncome(incomeList)

            // 8. Settings
            if (root.has("settings")) {
                val arr = root.getJSONArray("settings")
                if (arr.length() > 0) {
                    val oo = arr.getJSONObject(0)
                    val s = AppSettings(
                        id = oo.optInt("id", 1),
                        preferredLanguage = oo.optString("preferredLanguage", "en"),
                        darkMode = oo.optBoolean("darkMode", false),
                        currencySymbol = oo.optString("currencySymbol", "₹")
                    )
                    saveSettings(s)
                }
            }

            true
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }
}
