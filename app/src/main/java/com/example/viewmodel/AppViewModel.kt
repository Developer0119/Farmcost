package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = FarmCostRepository(db)

    // REACTIVE STATEFLOWS FROM ROOM
    val user: StateFlow<User?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val farms: StateFlow<List<Farm>> = repository.farmsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val crops: StateFlow<List<Crop>> = repository.cropsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.expensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workers: StateFlow<List<Worker>> = repository.workersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<Attendance>> = repository.attendanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val income: StateFlow<List<Income>> = repository.incomeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // UI TRANSIENT STATES
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _aiAdvice = MutableStateFlow("")
    val aiAdvice: StateFlow<String> = _aiAdvice.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _userProposedApiKey = MutableStateFlow("")
    val userProposedApiKey: StateFlow<String> = _userProposedApiKey.asStateFlow()

    init {
        // Initialize settings with basic values
        viewModelScope.launch {
            repository.insertSettings(AppSettings())
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCustomApiKey(key: String) {
        _userProposedApiKey.value = key
    }

    // PROFILE CRUD
    fun saveProfile(fullName: String, mobile: String, village: String, taluka: String, district: String, state: String, lang: String) {
        viewModelScope.launch {
            val updatedUser = User(
                id = 1,
                fullName = fullName,
                mobileNumber = mobile,
                village = village,
                taluka = taluka,
                district = district,
                state = state,
                preferredLanguage = lang
            )
            repository.insertUser(updatedUser)
            // Sync settings representation too
            val currSettings = settings.value
            repository.insertSettings(currSettings.copy(preferredLanguage = lang))
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            user.value?.let {
                repository.deleteUser(it)
            }
        }
    }

    // FARMS CRUD
    fun addFarm(name: String, area: Double, areaUnit: String, village: String, soilType: String, irrigationType: String, notes: String) {
        viewModelScope.launch {
            repository.insertFarm(
                Farm(
                    name = name,
                    area = area,
                    areaUnit = areaUnit,
                    village = village,
                    soilType = soilType,
                    irrigationType = irrigationType,
                    notes = notes
                )
            )
        }
    }

    fun updateFarm(id: Int, name: String, area: Double, areaUnit: String, village: String, soilType: String, irrigationType: String, notes: String) {
        viewModelScope.launch {
            repository.updateFarm(
                Farm(
                    id = id,
                    name = name,
                    area = area,
                    areaUnit = areaUnit,
                    village = village,
                    soilType = soilType,
                    irrigationType = irrigationType,
                    notes = notes
                )
            )
        }
    }

    fun deleteFarm(id: Int) {
        viewModelScope.launch {
            repository.deleteFarm(id)
        }
    }

    // CROPS CRUD
    fun addCrop(name: String, variety: String, season: String, farmId: Int, plantingDate: Long, expectedHarvestDate: Long) {
        viewModelScope.launch {
            repository.insertCrop(
                Crop(
                    name = name,
                    variety = variety,
                    season = season,
                    farmId = farmId,
                    plantingDate = plantingDate,
                    expectedHarvestDate = expectedHarvestDate
                )
            )
        }
    }

    fun updateCrop(id: Int, name: String, variety: String, season: String, farmId: Int, plantingDate: Long, expectedHarvestDate: Long) {
        viewModelScope.launch {
            repository.updateCrop(
                Crop(
                    id = id,
                    name = name,
                    variety = variety,
                    season = season,
                    farmId = farmId,
                    plantingDate = plantingDate,
                    expectedHarvestDate = expectedHarvestDate
                )
            )
        }
    }

    fun deleteCrop(id: Int) {
        viewModelScope.launch {
            repository.deleteCrop(id)
        }
    }

    // EXPENSES CRUD
    fun addExpense(date: Long, farmId: Int, cropId: Int, category: String, amount: Double, notes: String, receiptPic: String? = null) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    date = date,
                    farmId = farmId,
                    cropId = cropId,
                    category = category,
                    amount = amount,
                    notes = notes,
                    receiptPhotoPath = receiptPic
                )
            )
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    // WORKER CRUD
    fun addWorker(name: String, mobileNumber: String, workType: String) {
        viewModelScope.launch {
            repository.insertWorker(
                Worker(
                    name = name,
                    mobileNumber = mobileNumber,
                    workType = workType
                )
            )
        }
    }

    fun deleteWorker(id: Int) {
        viewModelScope.launch {
            repository.deleteWorker(id)
        }
    }

    // ATTENDANCE CRUD
    fun addAttendance(date: Long, workerId: Int, wage: Double, paidStatus: Boolean) {
        viewModelScope.launch {
            repository.insertAttendance(
                Attendance(
                    date = date,
                    workerId = workerId,
                    wage = wage,
                    paidStatus = paidStatus
                )
            )
        }
    }

    fun deleteAttendance(id: Int) {
        viewModelScope.launch {
            repository.deleteAttendance(id)
        }
    }

    // INCOME CRUD
    fun addIncome(cropId: Int, quantity: Double, unit: String, rate: Double, buyerName: String, saleDate: Long) {
        viewModelScope.launch {
            val calculatedAmount = quantity * rate
            repository.insertIncome(
                Income(
                    cropId = cropId,
                    quantity = quantity,
                    unit = unit,
                    rate = rate,
                    buyerName = buyerName,
                    saleDate = saleDate,
                    amount = calculatedAmount
                )
            )
        }
    }

    fun deleteIncome(id: Int) {
        viewModelScope.launch {
            repository.deleteIncome(id)
        }
    }

    // SETTINGS MUTATORS
    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            val curr = settings.value
            repository.insertSettings(curr.copy(preferredLanguage = lang))
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val curr = settings.value
            repository.insertSettings(curr.copy(darkMode = enabled))
        }
    }

    fun updateCurrency(symbol: String) {
        viewModelScope.launch {
            val curr = settings.value
            repository.insertSettings(curr.copy(currencySymbol = symbol))
        }
    }

    // BACKUP & RESTORE TRIGGERS
    suspend fun getBackupJson(): String {
        return repository.generateBackupJsonString()
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean {
        val success = repository.restoreFromJsonString(jsonString)
        if (success) {
            // Force screen refresh returning to dashboard
            _currentScreen.value = "dashboard"
        }
        return success
    }

    // GEMINI ADVISOR TRGGER
    fun askGeminiAdvisor() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiAdvice.value = "Gemini AI calculations are processing..."
            
            // Resolve API Key: use custom settings API key if provided, else fallback to injected key
            var keyToUse = _userProposedApiKey.value
            if (keyToUse.isEmpty()) {
                keyToUse = BuildConfig.GEMINI_API_KEY
            }

            val adviceResult = GeminiAdvisor.getAdvice(
                apiKey = keyToUse,
                language = settings.value.preferredLanguage,
                farmsList = farms.value,
                cropsList = crops.value,
                expensesList = expenses.value,
                incomeList = income.value
            )
            _aiAdvice.value = adviceResult
            _isAiLoading.value = false
        }
    }

    // SEEDS MOCK INJECTOR (So farmers start with some records immediately!)
    fun injectDemoData() {
        viewModelScope.launch {
            // Ensure no duplicate profiles are made
            val existingFarms = farms.value
            if (existingFarms.isNotEmpty()) return@launch

            // Save demo user
            saveProfile(
                fullName = "Rajesh Patel",
                mobile = "9876543210",
                village = "Pipalia",
                taluka = "Amod",
                district = "Bharuch",
                state = "Gujarat",
                lang = settings.value.preferredLanguage
            )

            // Save demo farms
            addFarm("Ganga Field", 3.5, "Acre", "Pipalia", "Black Cotton Soil", "Drip Irrigation", "Main field for cotton and wheat")
            addFarm("Yamuna Silt Valley", 1.8, "Hectare", "Pipalia", "Alluvial Sandy Silt", "Canal Irrigation", "River basin organic farm")

            // Wait a brief moment to get active farm IDs
            // Demo data generator with sequential inserts
            val defaultFarmId1 = 1
            val defaultFarmId2 = 2

            // Save demo crops
            addCrop("Bt Cotton Hybrid", "Bollgard II", "Kharif", defaultFarmId1, System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000, System.currentTimeMillis() + 45L * 24 * 60 * 60 * 1000)
            addCrop("Premium Wheat", "Lok-1 Sharbati", "Rabi", defaultFarmId2, System.currentTimeMillis() - 100L * 24 * 60 * 60 * 1000, System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000)

            // Save expenses
            addExpense(System.currentTimeMillis() - 50L * 24 * 60 * 60 * 1000, defaultFarmId1, 1, "Seeds", 4500.0, "Cotton seeds bags")
            addExpense(System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000, defaultFarmId1, 1, "Fertilizers", 12800.0, "Urea and NPK bags")
            addExpense(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, defaultFarmId1, 1, "Labor", 6400.0, "Sowing and weeding wages")
            addExpense(System.currentTimeMillis() - 20L * 24 * 60 * 60 * 1000, defaultFarmId1, 1, "Pesticides", 3800.0, "Neem oil organic spray")
            addExpense(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000, defaultFarmId1, 1, "Transport", 2100.0, "Tractor rental to town")

            // Yamuna Wheat expenses
            addExpense(System.currentTimeMillis() - 95L * 24 * 60 * 60 * 1000, defaultFarmId2, 2, "Seeds", 2900.0, "High yield organic wheat grain")
            addExpense(System.currentTimeMillis() - 80L * 24 * 60 * 60 * 1000, defaultFarmId2, 2, "Fertilizers", 8500.0, "Compost organic fertilizer")
            addExpense(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000, defaultFarmId2, 2, "Harvesting", 7600.0, "Combine harvester daily hire fee")

            // Save incomes
            addIncome(2, 45.0, "Quintal", 2100.0, "Government APMC Market Mandi", System.currentTimeMillis() - 4L * 24 * 60 * 60 * 1000)

            // Workers
            addWorker("Ramesh Kumar", "7896541230", "Sowing & Spraying")
            addWorker("Savitaben Rathod", "6543217890", "Manual Weeding")

            // Attendance
            addAttendance(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, 1, 350.0, true)
            addAttendance(System.currentTimeMillis() - 29L * 24 * 60 * 60 * 1000, 1, 350.0, true)
            addAttendance(System.currentTimeMillis() - 28L * 24 * 60 * 60 * 1000, 2, 300.0, false)
        }
    }
}
