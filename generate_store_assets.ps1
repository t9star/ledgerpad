Add-Type -AssemblyName System.Drawing

function Create-FeatureGraphic {
    param([string]$path)
    $w = 1024
    $h = 500
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    # Background gradient
    $rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.ColorTranslator]::FromHtml("#0D47A1"),
        [System.Drawing.ColorTranslator]::FromHtml("#1B5E20"),
        25.0
    )
    $g.FillRectangle($brush, $rect)

    # Typography
    $titleFont = New-Object System.Drawing.Font("Arial", 52, [System.Drawing.FontStyle]::Bold)
    $subFont = New-Object System.Drawing.Font("Arial", 22, [System.Drawing.FontStyle]::Regular)
    $badgeFont = New-Object System.Drawing.Font("Arial", 15, [System.Drawing.FontStyle]::Bold)

    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $subBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#E0E0E0"))

    # Title
    $g.DrawString("LedgerPad", $titleFont, $whiteBrush, 70, 110)

    # Subtitles (plain ASCII bullet points to avoid encoding artifacts)
    $g.DrawString("Smart Credit Ledger & Cashbook", $subFont, $whiteBrush, 75, 195)
    $g.DrawString("Udhaar | Fiado | Kasbon | 100% Offline", $subFont, $subBrush, 75, 240)

    # Badges
    $bY = 320
    $b1 = New-Object System.Drawing.Rectangle(75, $bY, 260, 42)
    $b2 = New-Object System.Drawing.Rectangle(350, $bY, 260, 42)
    $b3 = New-Object System.Drawing.Rectangle(75, ($bY + 52), 260, 42)
    $b4 = New-Object System.Drawing.Rectangle(350, ($bY + 52), 260, 42)

    $bBg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(60, 255, 255, 255))
    $g.FillRectangle($bBg, $b1)
    $g.FillRectangle($bBg, $b2)
    $g.FillRectangle($bBg, $b3)
    $g.FillRectangle($bBg, $b4)

    $g.DrawString("[OK] Zero Running Cost", $badgeFont, $whiteBrush, 85, ($bY + 11))
    $g.DrawString("[OK] No Account Needed", $badgeFont, $whiteBrush, 360, ($bY + 11))
    $g.DrawString("[OK] WhatsApp Receipts", $badgeFont, $whiteBrush, 85, ($bY + 63))
    $g.DrawString("[OK] Auto Cloud Backup", $badgeFont, $whiteBrush, 360, ($bY + 63))

    # Right Card Illustration
    $icX = 670
    $icY = 70
    $icW = 280
    $icH = 360
    $cardPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $cardPath.AddArc($icX, $icY, 32, 32, 180, 90)
    $cardPath.AddArc($icX + $icW - 32, $icY, 32, 32, 270, 90)
    $cardPath.AddArc($icX + $icW - 32, $icY + $icH - 32, 32, 32, 0, 90)
    $cardPath.AddArc($icX, $icY + $icH - 32, 32, 32, 90, 90)
    $cardPath.CloseFigure()

    $cardFill = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillPath($cardFill, $cardPath)

    # Spine
    $spFill = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#EEEEEE"))
    $g.FillRectangle($spFill, ($icX + 24), $icY, 8, $icH)

    # Entries
    $gFill = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#2E7D32"))
    $rFill = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#C62828"))

    $g.FillRectangle($gFill, ($icX + 50), ($icY + 60), 170, 24)
    $g.FillRectangle($rFill, ($icX + 50), ($icY + 110), 140, 24)
    $g.FillRectangle($gFill, ($icX + 50), ($icY + 160), 190, 24)
    $g.FillRectangle($rFill, ($icX + 50), ($icY + 210), 120, 24)
    $g.FillRectangle($gFill, ($icX + 50), ($icY + 260), 160, 24)

    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

Create-FeatureGraphic "store_assets\feature_graphic_1024x500.png"
Write-Output "Regenerated feature graphic!"
