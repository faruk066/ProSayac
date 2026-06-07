import re

# Read the file
with open('c:/Users/UnknownBoy/Desktop/projelerim/ProSayac/app/src/main/java/com/prosayac/app/util/serial/MBusProtocolHandler.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the old pollMeter return logic with the new one that returns both energy and volume
old_block = '''            // Determine unit based on meter type
            val isWaterMeter = if (response.size > 14) {
                val medium = response[14].toInt() and 0xFF
                medium == MEDIUM_WARM_WATER || medium == MEDIUM_COLD_WATER
            } else {
                false
            }

            val value = if (isWaterMeter) result.volume else result.energy
            val unit = if (isWaterMeter) "m³" else "kWh"

            return PollOutcome.Success(result.meterId ?: serialNumber, value, unit)'''

new_block = '''            // Return both energy and volume; the caller (MetersViewModel) will select
            // the appropriate value based on the database meter type
            return PollOutcome.Success(
                meterId = result.meterId ?: serialNumber,
                value = result.volume,
                unit = "m³",
                energy = result.energy,
                volume = result.volume
            )'''

content = content.replace(old_block, new_block)

# Write the file back
with open('c:/Users/UnknownBoy/Desktop/projelerim/ProSayac/app/src/main/java/com/prosayac/app/util/serial/MBusProtocolHandler.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done!")