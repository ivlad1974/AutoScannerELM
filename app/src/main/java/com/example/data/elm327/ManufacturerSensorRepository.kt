package com.example.data.elm327

import com.example.data.ObdSensor

data class VehicleManufacturerProfile(
    val id: String,
    val name: String,
    val description: String,
    val targetEcuHeader: String = "7E0",
    val sensors: List<ObdSensor> = emptyList()
) {
    val defaultHeader: String get() = targetEcuHeader
}

typealias ManufacturerProfile = VehicleManufacturerProfile

object ManufacturerSensorRepository {

    val availableProfiles: List<VehicleManufacturerProfile> by lazy {
        listOf(
            createToyotaHybridProfile(),
            createVagProfile(),
            createFordMazdaProfile(),
            createGmProfile(),
            createRenaultLadaProfile(),
            createHyundaiKiaProfile(),
            createJ1939HeavyDutyProfile()
        )
    }

    fun getAllProfiles(): List<VehicleManufacturerProfile> = availableProfiles

    fun getProfileById(id: String): VehicleManufacturerProfile? =
        availableProfiles.firstOrNull { it.id.equals(id, ignoreCase = true) }

    private fun createToyotaHybridProfile(): VehicleManufacturerProfile {
        val sensors = listOf(
            ObdSensor(
                pid = "2129",
                name = "Заряд тяговой высоковольтной батареи (SOC)",
                shortName = "HV Batt SOC",
                value = 60.5,
                unit = "%",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Гибрид и Электро",
                formula = "A / 2",
                ecuHeader = "7E2",
                mode = "21",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "Toyota / Lexus Hybrid",
                description = "Фактический уровень заряда высоковольтной батареи Toyota Prius / Camry / RAV4 Hybrid. Рабочая зона: 40–80%."
            ),
            ObdSensor(
                pid = "2182",
                name = "Температура инвертора мотор-генератора MG1",
                shortName = "Inverter MG1",
                value = 45.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 150.0,
                category = "Гибрид и Электро",
                formula = "B - 40",
                ecuHeader = "7E2",
                mode = "21",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "Toyota / Lexus Hybrid",
                description = "Температура силового инвертора генератора MG1 силовой установки THS."
            ),
            ObdSensor(
                pid = "2183",
                name = "Температура инвертора электромотора MG2",
                shortName = "Inverter MG2",
                value = 48.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 150.0,
                category = "Гибрид и Электро",
                formula = "B - 40",
                ecuHeader = "7E2",
                mode = "21",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "Toyota / Lexus Hybrid",
                description = "Температура силовых ключей главного тягового электромотора MG2."
            ),
            ObdSensor(
                pid = "21C3",
                name = "Ток высоковольтной батареи (HV Amps)",
                shortName = "HV Current",
                value = 0.0,
                unit = "А",
                minVal = -150.0,
                maxVal = 150.0,
                category = "Гибрид и Электро",
                formula = "((256 * A + B) - 32768) / 100",
                ecuHeader = "7E2",
                mode = "21",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "Toyota / Lexus Hybrid",
                description = "Ток заряда (+) и разряда (-) тяговой батареи. При рекуперативном торможении до +120 А."
            ),
            ObdSensor(
                pid = "21C4",
                name = "Общее напряжение высоковольтной батареи (HV Pack)",
                shortName = "HV Pack Volt",
                value = 212.0,
                unit = "В",
                minVal = 150.0,
                maxVal = 260.0,
                category = "Гибрид и Электро",
                formula = "(256 * A + B) / 10",
                ecuHeader = "7E2",
                mode = "21",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "Toyota / Lexus Hybrid",
                description = "Суммарное напряжение всех секций батареи гибридной системы."
            )
        )
        return VehicleManufacturerProfile(
            id = "toyota_hybrid",
            name = "Toyota & Lexus Hybrid (THS-II / Prius / RAV4)",
            description = "Параметры высоковольтной батареи, инверторов MG1/MG2, токи рекуперации и SOC",
            targetEcuHeader = "7E2",
            sensors = sensors
        )
    }

    private fun createVagProfile(): VehicleManufacturerProfile {
        val sensors = listOf(
            ObdSensor(
                pid = "221154",
                name = "Температура сцепления коробки передач DSG (DQ200/DQ250)",
                shortName = "DSG Clutch Temp",
                value = 65.0,
                unit = "°C",
                minVal = 0.0,
                maxVal = 250.0,
                category = "Трансмиссия",
                formula = "(256 * A + B) / 10 - 40",
                ecuHeader = "7E1",
                mode = "22",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "VAG (VW/Audi/Skoda)",
                description = "Температура фрикционных дисков робота DSG. Перегрев начинается выше 160°C."
            ),
            ObdSensor(
                pid = "221940",
                name = "Масса сажи в сажевом фильтре DPF (Soot Mass)",
                shortName = "DPF Soot Mass",
                value = 12.8,
                unit = "г",
                minVal = 0.0,
                maxVal = 70.0,
                category = "Выхлоп и Экология",
                formula = "(256 * A + B) / 100",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "VAG (VW/Audi/Skoda)",
                description = "Фактическое накопление сажи в граммах. При превышении 24 г включается авторегенерация."
            ),
            ObdSensor(
                pid = "221160",
                name = "Фактическое давление наддува турбокомпрессора (TSI/TDI)",
                shortName = "Actual Boost",
                value = 1.05,
                unit = "бар",
                minVal = 0.0,
                maxVal = 3.5,
                category = "Впуск и Турбо",
                formula = "(256 * A + B) / 1000",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "VAG (VW/Audi/Skoda)",
                description = "Абсолютное давление наддува турбины для моторов TSI, TFSI и TDI."
            ),
            ObdSensor(
                pid = "222B00",
                name = "Уровень моторного масла в картере (датчик TOG)",
                shortName = "Oil Level mm",
                value = 68.0,
                unit = "мм",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Смазка и Масло",
                formula = "A",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "VAG (VW/Audi/Skoda)",
                description = "Электронный замер уровня масла датчиком в картере двигателя (норма: 55–75 мм)."
            )
        )
        return VehicleManufacturerProfile(
            id = "vag_extended",
            name = "VAG Group (Volkswagen, Audi, Skoda, Seat)",
            description = "Параметры коробок DSG, сажевых фильтров TDI, наддува TSI и уровня масла TOG",
            targetEcuHeader = "7E0",
            sensors = sensors
        )
    }

    private fun createFordMazdaProfile(): VehicleManufacturerProfile {
        val sensors = listOf(
            ObdSensor(
                pid = "22162E",
                name = "Температура масла в АКПП (TFT Ford / Mazda)",
                shortName = "Ford Trans Temp",
                value = 75.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 160.0,
                category = "Трансмиссия",
                formula = "((256 * A + B) / 100) - 40",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "Ford / Mazda",
                description = "Температура трансмиссионной жидкости автоматической коробки передач (6F35, 6R80, 10R80)."
            ),
            ObdSensor(
                pid = "221624",
                name = "Температура головки блока цилиндров (CHT Ford)",
                shortName = "Ford CHT",
                value = 92.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 215.0,
                category = "Охлаждение",
                formula = "A - 40",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "Ford / Mazda",
                description = "Температура металла головки блока (датчик CHT реагирует быстрее антифриза при утечке ОЖ)."
            ),
            ObdSensor(
                pid = "221E1C",
                name = "Коэффициент проскальзывания гидротрансформатора",
                shortName = "Torque Conv Slip",
                value = 12.0,
                unit = "об/мин",
                minVal = 0.0,
                maxVal = 1500.0,
                category = "Трансмиссия",
                formula = "(256 * A + B) / 4",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 2,
                isSelected = false,
                isCustom = true,
                profileName = "Ford / Mazda",
                description = "Разница оборотов между двигателем и турбинным валом коробки (блокировка ГДТ)."
            )
        )
        return VehicleManufacturerProfile(
            id = "ford_mazda",
            name = "Ford & Mazda (Focus, Kuga, Mondeo, Explorer)",
            description = "Температура АКПП, датчик головки блока CHT и муфта полного привода",
            targetEcuHeader = "7E0",
            sensors = sensors
        )
    }

    private fun createGmProfile(): VehicleManufacturerProfile {
        val sensors = listOf(
            ObdSensor(
                pid = "221940",
                name = "Температура рабочей жидкости АКПП (GM 6T40 / 6T45 / 8L90)",
                shortName = "GM Trans Temp",
                value = 82.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 150.0,
                category = "Трансмиссия",
                formula = "A - 40",
                ecuHeader = "7E1",
                mode = "22",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "General Motors",
                description = "Температура трансмиссионного масла Dexron VI в гидроблоке АКПП GM."
            ),
            ObdSensor(
                pid = "221154",
                name = "Остаточный ресурс моторного масла (Oil Life Remaining)",
                shortName = "GM Oil Life",
                value = 78.0,
                unit = "%",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Смазка и Масло",
                formula = "A * 100 / 255",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "General Motors",
                description = "Расчетный ресурс моторного масла по алгоритмам GM Oil Life System."
            ),
            ObdSensor(
                pid = "221942",
                name = "Фактическое давление топлива в рампе непосредственного впрыска",
                shortName = "GM Direct Inj Press",
                value = 4.2,
                unit = "МПа",
                minVal = 0.0,
                maxVal = 25.0,
                category = "Топливоподача",
                formula = "(256 * A + B) * 0.01",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 2,
                isSelected = false,
                isCustom = true,
                profileName = "General Motors",
                description = "Давление в рампе непосредственного впрыска GM Ecotec Turbo / DI."
            )
        )
        return VehicleManufacturerProfile(
            id = "gm_chevrolet",
            name = "General Motors (Chevrolet, Opel, Buick, Cadillac)",
            description = "Температура АКПП 6T40, остаточный ресурс масла GM Oil Life и давление GDI",
            targetEcuHeader = "7E0",
            sensors = sensors
        )
    }

    private fun createRenaultLadaProfile(): VehicleManufacturerProfile {
        val sensors = listOf(
            ObdSensor(
                pid = "222004",
                name = "Температура масла коробки передач (Lada Vesta / XRAY / Duster)",
                shortName = "Lada Trans Temp",
                value = 72.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 150.0,
                category = "Трансмиссия",
                formula = "A - 40",
                ecuHeader = "7E1",
                mode = "22",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "Renault / Lada",
                description = "Температура трансмиссионного масла в блоке АМТ (робот) или вариатора Jatco CVT."
            ),
            ObdSensor(
                pid = "222006",
                name = "Давление хладагента в контуре кондиционера (фреон)",
                shortName = "A/C Freon Press",
                value = 8.5,
                unit = "бар",
                minVal = 0.0,
                maxVal = 30.0,
                category = "Климат и Кузов",
                formula = "(256 * A + B) / 100",
                ecuHeader = "7E0",
                mode = "22",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "Renault / Lada",
                description = "Давление в магистрали высокого давления климатической системы (норма 6–16 бар)."
            ),
            ObdSensor(
                pid = "2101",
                name = "Счетчик детонации двигателя (Knock Counter)",
                shortName = "Knock Counter",
                value = 0.0,
                unit = "событий",
                minVal = 0.0,
                maxVal = 255.0,
                category = "Зажигание",
                formula = "A",
                ecuHeader = "7E0",
                mode = "21",
                bytesCount = 1,
                isSelected = false,
                isCustom = true,
                profileName = "Renault / Lada",
                description = "Фиксация детонационного сгорания топлива по датчику детонации блоком Итэлма/Continental."
            )
        )
        return VehicleManufacturerProfile(
            id = "renault_lada",
            name = "Renault & Lada (Vesta, Granta, Logan, Duster)",
            description = "Параметры блоков EMS3120/Итэлма: АКПП/CVT, давление кондиционера, детонация",
            targetEcuHeader = "7E0",
            sensors = sensors
        )
    }

    private fun createHyundaiKiaProfile(): VehicleManufacturerProfile {
        val sensors = listOf(
            ObdSensor(
                pid = "2102",
                name = "Температура масла в АКПП (Hyundai / Kia ATF)",
                shortName = "HK ATF Temp",
                value = 78.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 150.0,
                category = "Трансмиссия",
                formula = "A - 40",
                ecuHeader = "7E1",
                mode = "21",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "Hyundai / Kia",
                description = "Температура жидкости в гидромеханической 6-ст коробке A6MF/A6LF."
            ),
            ObdSensor(
                pid = "2105",
                name = "Давление в шинах TPMS (Переднее левое)",
                shortName = "TPMS FL",
                value = 2.3,
                unit = "бар",
                minVal = 0.0,
                maxVal = 4.0,
                category = "Климат и Кузов",
                formula = "A * 0.0275",
                ecuHeader = "7D0",
                mode = "21",
                bytesCount = 1,
                isSelected = false,
                isCustom = true,
                profileName = "Hyundai / Kia",
                description = "Прямые показания беспроводного датчика давления в переднем левом колесе."
            )
        )
        return VehicleManufacturerProfile(
            id = "hyundai_kia",
            name = "Hyundai & Kia (Solaris, Rio, Creta, Sportage, Tucson)",
            description = "Температура АКПП A6, давление в колесах TPMS, статус муфты AWD",
            targetEcuHeader = "7E1",
            sensors = sensors
        )
    }

    private fun createJ1939HeavyDutyProfile(): VehicleManufacturerProfile {
        val sensors = listOf(
            ObdSensor(
                pid = "FEEF",
                name = "Давление масла двигателя (SAE J1939 Heavy Duty)",
                shortName = "J1939 Oil Press",
                value = 380.0,
                unit = "кПа",
                minVal = 0.0,
                maxVal = 1000.0,
                category = "Смазка и Масло",
                formula = "A * 4",
                ecuHeader = "7E0",
                mode = "00",
                bytesCount = 1,
                isSelected = true,
                isCustom = true,
                profileName = "SAE J1939 Trucks",
                description = "Манометрическое давление масла тяжелых грузовиков и спецтехники (PGN 65263)."
            ),
            ObdSensor(
                pid = "FEF7",
                name = "Напряжение бортовой сети 24В (J1939)",
                shortName = "24V Battery",
                value = 27.6,
                unit = "В",
                minVal = 16.0,
                maxVal = 32.0,
                category = "Электрика",
                formula = "(256 * A + B) * 0.05",
                ecuHeader = "7E0",
                mode = "00",
                bytesCount = 2,
                isSelected = true,
                isCustom = true,
                profileName = "SAE J1939 Trucks",
                description = "Напряжение генератора и аккумуляторов грузовой 24-вольтовой сети (PGN 65271)."
            )
        )
        return VehicleManufacturerProfile(
            id = "sae_j1939",
            name = "SAE J1939 (Грузовики, КАМАЗ, Cummins, MAN, Scania)",
            description = "Стандарт коммерческого транспорта CAN 29bit 250k: давление масла, 24В сеть, PGN параметры",
            targetEcuHeader = "7E0",
            sensors = sensors
        )
    }

    /**
     * Parses standard Torque Pro compatible CSV files.
     * Expected columns format:
     * Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header
     */
    fun parseTorqueCsv(csvContent: String, profileName: String = "Импортированный профиль"): List<ObdSensor> {
        val result = mutableListOf<ObdSensor>()
        val lines = csvContent.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue
            val parts = trimmed.split(",").map { it.trim().removeSurrounding("\"") }
            if (parts.size < 7) continue

            // Check header row
            if (parts[0].equals("Name", ignoreCase = true) || parts[2].equals("ModeAndPID", ignoreCase = true)) continue

            try {
                val name = parts[0]
                val shortName = parts[1]
                val modeAndPid = parts[2].replace(" ", "").uppercase()
                val equation = parts[3]
                val minVal = parts[4].toDoubleOrNull() ?: 0.0
                val maxVal = parts[5].toDoubleOrNull() ?: 100.0
                val units = parts[6]
                val header = if (parts.size >= 8 && parts[7].isNotBlank()) parts[7].uppercase() else "7E0"

                val (mode, pid) = if (modeAndPid.length >= 4) {
                    Pair(modeAndPid.substring(0, 2), modeAndPid)
                } else {
                    Pair("01", "01$modeAndPid")
                }

                result.add(
                    ObdSensor(
                        pid = pid,
                        name = name,
                        shortName = shortName,
                        value = 0.0,
                        unit = units,
                        minVal = minVal,
                        maxVal = maxVal,
                        category = "Пользовательские PID",
                        formula = equation,
                        ecuHeader = header,
                        mode = mode,
                        bytesCount = 2,
                        isSelected = true,
                        isCustom = true,
                        profileName = profileName,
                        description = "Импортированный датчик из Torque CSV: $name ($pid, заголовок $header)."
                    )
                )
            } catch (e: Exception) {
                // Ignore malformed rows
            }
        }
        return result
    }
}
