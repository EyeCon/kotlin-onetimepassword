import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.geometry.Insets
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.util.Duration
import org.apache.commons.codec.binary.Base32
import tornadofx.*
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime


class EasyGridPane : GridPane() {
  private var _row = 0
  fun row(node1: javafx.scene.Node? = null, node2: javafx.scene.Node? = null, colspan: Int = 1, rowspan: Int = 1) {
    if (node1 != null) add(node1, 0, _row, colspan, rowspan)
    if (node2 != null) add(node2, 1, _row, colspan, rowspan)
    _row++
  }
}


class CodeGeneratorView : View() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val QR_CODE_SIZE = 180
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  override val root = EasyGridPane()

  private var plainTextSecret = "Secret1234".toByteArray(StandardCharsets.UTF_8)

  private val plainTextSecretTextField = TextField(plainTextSecret.toHex()).apply { isEditable = false }
  private val base32encodedSecretTextField = TextField().apply { isEditable = false }
  private val base32encodedSecretQrCode = Canvas(QR_CODE_SIZE.toDouble(), QR_CODE_SIZE.toDouble())
  private val codeTextField = TextField().apply { isEditable = false }
  private val codeValidityProgressIndicator = ProgressBar(0.0)
  private val windowSpinner = Spinner<Int>(0, 10, 1)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {

    title = "Google Authenticator Example"

    root.paddingAll = 20
    root.hgap = 4.0
    root.vgap = 4.0

//    var row = 0
    root.row(Label("Must have 10 characters to work correctly with most generator apps."), rowspan = 1, colspan = 2)
    root.row(Label("Plain text secret (in hex):"), plainTextSecretTextField)
    plainTextSecretTextField.textProperty().addListener { _, _, newValue ->
      if (newValue.isBlank()) {
        return@addListener
      }
      generateGoogleAuthenticatorCode()
      refreshQrCode()
    }

    val addSeparator : () -> Separator = {
      val separator = Separator()
      root.row(separator, colspan = 2)
      GridPane.setMargin(separator, Insets(8.0, 0.0, 8.0, 0.0))
      separator
    }
    addSeparator()

    root.row(Label("This is the secret that must be used in the generator apps."), colspan = 2)
    root.row(Label("Base32-encoded secret:"), base32encodedSecretTextField)
    root.row(null, Button("Generate random").apply {
      setOnAction {
        val createRandomSecret = GoogleAuthenticator.createRandomSecret()
        plainTextSecret = Base32().decode(createRandomSecret.toByteArray(StandardCharsets.UTF_8))
        plainTextSecretTextField.text = plainTextSecret.toHex()
        generateGoogleAuthenticatorCode()
        refreshQrCode()
      }
    })
    root.row(null, base32encodedSecretQrCode)

    addSeparator()
    root.row(Label("Window:"), windowSpinner)
    windowSpinner.valueProperty().addListener { _, _, _ ->
      generateGoogleAuthenticatorCode()
      refreshQrCode()
    }
    root.row(Label("Codes:"), codeTextField)
    root.row(Label("Code validity:"), codeValidityProgressIndicator)
    val codeValidityUpdate = Timeline(KeyFrame(Duration.seconds(1.0), {
      generateGoogleAuthenticatorCode()
    }))
    codeValidityUpdate.cycleCount = Timeline.INDEFINITE
    codeValidityUpdate.play()

    // Initial generation
    generateGoogleAuthenticatorCode()
    refreshQrCode()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun generateGoogleAuthenticatorCode() {
    val base32secret = Base32().encodeToString(plainTextSecret)
    base32encodedSecretTextField.text = base32secret

    val googleAuthenticator = GoogleAuthenticator(base32secret)
    codeTextField.text = googleAuthenticator.generateWindow(windowSize = windowSpinner.value).joinToString()

    val second = (LocalDateTime.now().second) % 30
    codeValidityProgressIndicator.progress = 1.0 - (second / 30.0)
  }

  private fun refreshQrCode() {
    val base32secret = base32encodedSecretTextField.text
    if (base32secret.isBlank()) {
      return
    }

    // See https://github.com/google/google-authenticator/wiki/Key-Uri-Format
    val qrText = "otpauth://totp/me@company.com:?secret=$base32secret&issuer=GoogleAuthenticatorExample"
    val qrCodeMatrix = QRCodeWriter().encode(qrText, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE,
                                              mapOf(Pair(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L)))

    val graphics: GraphicsContext = base32encodedSecretQrCode.graphicsContext2D

    // Background
    graphics.fill = javafx.scene.paint.Color.WHITE
    graphics.fillRect(0.0, 0.0, QR_CODE_SIZE.toDouble(), QR_CODE_SIZE.toDouble())

    // QR code
    graphics.fill = javafx.scene.paint.Color.BLACK
    for (i in 0 until qrCodeMatrix.width) {
      for (j in 0 until qrCodeMatrix.height) {
        if (qrCodeMatrix[i, j]) {
          graphics.fillRect(i.toDouble(), j.toDouble(), 1.0, 1.0)
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}

fun String.hexStringToByteArray(): ByteArray = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
fun ByteArray.toHex() = joinToString(separator = " ") { it.toInt().and(0xff).toString(16).padStart(2, '0') }

