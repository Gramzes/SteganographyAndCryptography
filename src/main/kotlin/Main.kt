package cryptography

import java.io.File
import javax.imageio.ImageIO
import java.awt.Color

fun main() {
    var lastCommand = ""

    while (lastCommand!="exit") {
        println("Task (hide, show, exit): ")
        lastCommand = readln()

        when (lastCommand) {
            "hide" -> {
                println("Input image file:")
                val inputFile = readln()
                println("Output image file:")
                val outputFile = readln()
                println("Message to hide:")
                val message = readln()
                println("Password:")
                val password = readln()
                try {
                    val input = File(inputFile)
                    val output = File(outputFile)
                    hide(input, output, message, password)
                    println("Message saved in $outputFile image.")
                }
                catch (ex : TooLargeMessage) {
                    println(ex.message)
                }
                catch (ex : Exception) {
                    println("Can't read input file!")
                }
            }
            "show" -> {
                println("Input image file:")
                val inputFile = readln()
                println("Password:")
                val password = readln()
                var message = ""
                try {
                    val input = File(inputFile)
                    message = show(input, password)
                }
                catch (ex : Exception){
                    println("Can't read input file!")
                }
                println("Message:")
                println(message)

            }
            "exit" -> println("Bye!")
            else -> println("Wrong task: $lastCommand")
        }
    }
}

fun hide(inputFile : File, outputFile : File, message : String, password: String) {
    val bufferedImage = ImageIO.read(inputFile)
    val endOfMessageBytes = byteArrayOf(0, 0, 3)
    val byteArray = endecrypt(message.encodeToByteArray(), password) + endOfMessageBytes
    val numberOfBitsInMes = byteArray.size * 8

    if (bufferedImage.width * bufferedImage.height < numberOfBitsInMes)
        throw TooLargeMessage("The input image is not large enough to hold this message.")

    var numberOfCurrentBit = 0
    loop@for (y in 0 until bufferedImage.height){
        for (x in 0 until bufferedImage.width){
            if (numberOfCurrentBit >= numberOfBitsInMes) break@loop

            val numberOfCurrentByte = numberOfCurrentBit / 8
            val currentByte = byteArray[numberOfCurrentByte]
            val currentBit = if (currentByte.toInt() and (1 shl (7 - numberOfCurrentBit % 8)) > 0) 1 else 0

            val oldColor = Color(bufferedImage.getRGB(x, y))
            val red = oldColor.red
            val green = oldColor.green
            val blue = if (currentBit == 0) {
                oldColor.blue - oldColor.blue % 2
            }
            else{
                oldColor.blue + ((oldColor.blue + 1) % 2)
            }

            val newColor = Color(red, green, blue)
            bufferedImage.setRGB(x, y, newColor.rgb)
            numberOfCurrentBit++
        }
    }
    outputFile.createNewFile()
    ImageIO.write(bufferedImage, "png", outputFile)
}

fun show(file : File, password: String) : String {
    val bufferedImage = ImageIO.read(file)
    var byteList = mutableListOf<Byte>()

    var numberOfCurrentBit = 0
    var currentByte = 0
    loop@for (y in 0 until bufferedImage.height){
        for (x in 0 until bufferedImage.width){
            val oldColor = Color(bufferedImage.getRGB(x, y))
            val blue = oldColor.blue

            val currentBit = blue and 1
            currentByte = currentByte or (currentBit shl (7 - numberOfCurrentBit % 8))

            if (numberOfCurrentBit % 8 == 7) {
                byteList.add(currentByte.toByte())
                currentByte = 0

                val numberOfCurrentByte = numberOfCurrentBit / 8
                if (byteList.size >= 3 && byteList[numberOfCurrentByte - 2] == 0.toByte()
                    && byteList[numberOfCurrentByte - 1] == 0.toByte()
                    && byteList[numberOfCurrentByte] == 3.toByte()
                ) {
                    byteList = byteList.subList(0, numberOfCurrentByte - 2)
                    break@loop
                }
            }
            numberOfCurrentBit++
        }
    }
    val byteArray = endecrypt(byteList.toByteArray(), password)
    return byteArray.toString(Charsets.UTF_8)
}

fun endecrypt(byteArray: ByteArray, password : String) : ByteArray {
    val passByteArray = password.encodeToByteArray()
    val newByteArray = ByteArray(byteArray.size)
    for (i in byteArray.indices){
        val currentPassByte = passByteArray[i % passByteArray.size]
        newByteArray[i] = (byteArray[i].toInt() xor currentPassByte.toInt()).toByte()
    }
    return newByteArray
}

class TooLargeMessage(message:String): Exception(message)