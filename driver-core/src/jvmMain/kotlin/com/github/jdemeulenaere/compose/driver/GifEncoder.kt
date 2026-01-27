package com.github.jdemeulenaere.compose.driver

import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.MemoryCacheImageOutputStream

/** Implemented by Gemini. */
internal fun generateGif(
    images: List<BufferedImage>,
    outputStream: OutputStream,
    frameDelayMs: Long,
) {
    if (images.isEmpty()) return

    val imageType = images[0].type
    val delayTime10ms = maxOf((frameDelayMs / 10).toInt(), 2)
    MemoryCacheImageOutputStream(outputStream).use { ios ->
        GifSequenceWriter(ios, imageType, delayTime10ms, true).use { writer ->
            for (image in images) {
                writer.writeToSequence(image)
            }
        }
    }
}

private class GifSequenceWriter(
    outputStream: javax.imageio.stream.ImageOutputStream,
    imageType: Int,
    delayTime10ms: Int,
    loopContinuously: Boolean,
) : AutoCloseable {
    private val writer = ImageIO.getImageWritersBySuffix("gif").next()
    private val params = writer.defaultWriteParam
    private val metadata: IIOMetadata

    init {
        val imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType)
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params)

        val metaFormatName = metadata.nativeMetadataFormatName
        val root = metadata.getAsTree(metaFormatName) as IIOMetadataNode

        val graphicsControlExtensionNode = getNode(root, "GraphicControlExtension")
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none")
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("delayTime", delayTime10ms.toString())
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0")

        val appExtensionsNode = getNode(root, "ApplicationExtensions")
        val child = IIOMetadataNode("ApplicationExtension")
        child.setAttribute("applicationID", "NETSCAPE")
        child.setAttribute("authenticationCode", "2.0")

        val loop = if (loopContinuously) 0 else 1
        child.userObject = byteArrayOf(0x1, (loop and 0xFF).toByte(), 0)
        appExtensionsNode.appendChild(child)

        metadata.setFromTree(metaFormatName, root)
        writer.output = outputStream
        writer.prepareWriteSequence(null)
    }

    fun writeToSequence(img: BufferedImage) {
        writer.writeToSequence(IIOImage(img, null, metadata), params)
    }

    override fun close() {
        writer.endWriteSequence()
    }

    private fun getNode(rootNode: IIOMetadataNode, nodeName: String): IIOMetadataNode {
        val nNodes = rootNode.length
        for (i in 0 until nNodes) {
            if (rootNode.item(i).nodeName.equals(nodeName, ignoreCase = true)) {
                return rootNode.item(i) as IIOMetadataNode
            }
        }
        val node = IIOMetadataNode(nodeName)
        rootNode.appendChild(node)
        return node
    }
}
