package com.elfefe.common.pager.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class News : PageImpl() {

    @Composable
    override fun Show() {
        var showFileBrowser by remember { mutableStateOf(false) }

        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                showFileBrowser = true
            }) {
                Icon(Icons.Default.AccountBox, "", Modifier.size(72.dp))
            }
        }
        if (showFileBrowser)
            FileBrowse(
                onFileSelected = {
                    editExcel(it.absolutePath)
                },
                onStateChange = {
                    showFileBrowser = it
                },false
            )
    }

    private fun editExcel(file: String) {
        val fileInpuString = FileInputStream(File(file))
        val workbook: Workbook = XSSFWorkbook(fileInpuString)

        val sheet = workbook.getSheetAt(0)

        sheet.createRow(2).run {
            createCell(2).run {
                setCellValue("OUI")
            }
        }

        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        workbook.close()
    }

    @Composable
    private fun FileBrowse(
        onFileSelected: (File) -> Unit,
        onStateChange: (Boolean) -> Unit,
        loadFile: Boolean = true
    ) {
        AwtWindow(
            visible = true,
            create = {
                object : FileDialog(ComposeWindow(), "Choose a file", if (loadFile) LOAD else SAVE) {
                    override fun setVisible(value: Boolean) {
                        super.setVisible(value)
                        if (value) {
                            onFileSelected(File(file))
                        }
                    }
                }
            },
            dispose = {
                onStateChange(false)
            },
            update = {}
        )
    }
}