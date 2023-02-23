package fr.exem.common.quality.s4

import fr.exem.common.pager.page.Excel
import fr.exem.common.utils.asString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.HashMap

class GeTe007 {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val runningJobs = hashMapOf<String, Job>()

    private val Workbook.userCellStyle: CellStyle
        get() = createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            bottomBorderColor = IndexedColors.LAVENDER.index
            borderTop = BorderStyle.THIN
            topBorderColor = IndexedColors.LAVENDER.index
            borderLeft = BorderStyle.THIN
            leftBorderColor = IndexedColors.LAVENDER.index
            borderRight = BorderStyle.THIN
            rightBorderColor = IndexedColors.LAVENDER.index

            fillPattern = FillPatternType.SOLID_FOREGROUND
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillBackgroundColor = IndexedColors.DARK_BLUE.index
        }

    private val Workbook.rightCellStyle: CellStyle
        get() = createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            bottomBorderColor = IndexedColors.TAN.index
            borderTop = BorderStyle.THIN
            topBorderColor = IndexedColors.TAN.index
            borderLeft = BorderStyle.THIN
            leftBorderColor = IndexedColors.TAN.index
            borderRight = BorderStyle.THIN
            rightBorderColor = IndexedColors.TAN.index

            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

    private fun clearUsersSheet(usersSheet: Sheet) {
        for (rowIndex in USERS_SHEET_FIRST_USER_ROW..usersSheet.lastRowNum)
            usersSheet.removeRow(usersSheet.getRow(rowIndex))
    }

    private fun getGroupsRows(groupsSheet: Sheet) =
        mutableListOf<Row>().apply {
            for (rowIndex in groupsSheet.firstRowNum..groupsSheet.lastRowNum)
                groupsSheet.getRow(rowIndex)?.let { row -> add(row) }
        }

    private fun getUsersWithPosts(groupsRows: List<Row>): HashMap<String, MutableList<String>> {
        return hashMapOf<String, MutableList<String>>().apply {
            groupsRows.forEach { row ->
                for (cellIndex in row.firstCellNum..row.lastCellNum) {
                    val cell = row.getCell(cellIndex)?.asString() ?: ""
                    if (cell.isNotBlank() && groupsRows.indexOf(row) > 1) {
                        val post = groupsRows[1].getCell(cellIndex).toString()

                        if (!containsKey(cell))
                            set(cell, mutableListOf(post))
                        else if (!get(cell)?.contains(post)!!)
                            get(cell)?.add(post)
                    }
                }
            }
        }
    }

    private fun getPostsRights(groupsRows: List<Row>, rightsSheet: Sheet): HashMap<String, MutableList<String>> {
        return hashMapOf<String, MutableList<String>>().apply {
            val secondRow = groupsRows[1]
            for (cellIndex in secondRow.firstCellNum..secondRow.lastCellNum)
                secondRow.getCell(cellIndex)?.run { set(asString(), mutableListOf()) }

            for (rowIndex in RIGHTS_SHEET_FIRST_RIGHT_ROW..rightsSheet.lastRowNum)
                rightsSheet.getRow(rowIndex)?.run {
                    getCell(0)?.let { cell ->
                        val post = cell.asString()
                        set(post, mutableListOf())
                        for (cellIndex in RIGHTS_SHEET_FIRST_RIGHT_COLUMN..lastCellNum)
                            getCell(cellIndex)?.run { get(post)?.add(asString()) }
                    }
                }
        }
    }

    private fun createCell(sheet: Sheet, cellReference: CellReference): Cell {
        val row = sheet
            .getRow(cellReference.row)
            ?: sheet.createRow(cellReference.row)
        return row
            ?.getCell(cellReference.col.toInt())
            ?: row.createCell(cellReference.col.toInt())
    }

    private fun generateRightsByUsers(
        users: Map<String, MutableList<String>>,
        postsRights: Map<String, MutableList<String>>,
        usersSheet: Sheet,
        wb: Workbook
    ) {
        users.onEachIndexed { index, (user, posts) ->
            val userIndex = index + USERS_SHEET_FIRST_USER_ROW
            val userNameCell = createCell(usersSheet, CellReference(userIndex, 0))
            userNameCell.setCellValue(user)
            userNameCell.cellStyle = wb.userCellStyle

            posts.forEach { post ->
                postsRights[post]?.forEachIndexed { index, right ->
                    val rightIndex = index + USERS_SHEET_FIRST_RIGHT_COLUMN
                    val currentRightCellReference = CellReference(userIndex, rightIndex)
                    val rightData = RIGHTS_VALUE[right]
                    val usersRightCell = createCell(usersSheet, currentRightCellReference)
                    val currentRightValue = usersRightCell.asString()

                    if (currentRightValue.isBlank()
                        || !RIGHTS_VALUE.containsKey(currentRightValue)
                        || (rightData?.get("value") ?: -1) > (RIGHTS_VALUE[currentRightValue]?.get("value") ?: -1)
                    ) {
                        val rightCellStyle = wb.rightCellStyle
                        rightData?.get("color")?.let { color ->
                            rightCellStyle.fillForegroundColor = color
                        }
                        usersRightCell.cellStyle = rightCellStyle
                        usersRightCell.setCellValue(right)
                    }
                }
            }
        }
    }

    private fun sortUsersByRights(users: Map<String, MutableList<String>>, postsRights: Map<String, MutableList<String>>): SortedMap<String, MutableList<String>> {
        return users.toSortedMap (compareByDescending<String> { name ->
            var rightest = 0
            val posts = users[name]
            posts?.forEach { post ->
                postsRights[post]
                    ?.filter { right -> right == "rw" }
                    ?.run {
                        if (isNotEmpty() && size > rightest) rightest = size
                    }
            }
            rightest
        }.thenBy { it })
    }

    fun generateUsers(fromPath: String, toPath: String, progress: (Excel.LogState) -> Unit) {
        if (!toPath.contains(EXCEL_FILE)) return progress(Excel.LogState(
            text = "The output file need to be named $EXCEL_FILE",
            status = Excel.LogState.Status.ERROR
        ))
        if (!runningJobs.containsKey(toPath) || runningJobs[toPath]?.isActive == false)
            runningJobs[toPath] = scope.launch(Dispatchers.IO) {
                try {
                    val wb: Workbook = XSSFWorkbook(FileInputStream(fromPath))
                    progress(Excel.LogState("Loaded $fromPath", Excel.LogState.Status.INFO))

                    val usersSheet = wb.getSheet(USERS_SHEET)
                    val rightsSheet = wb.getSheet(RIGHTS_SHEET)
                    val groupsSheet = wb.getSheet(GROUPS_SHEET)

                    clearUsersSheet(usersSheet)
                    progress(Excel.LogState("Cleared users", Excel.LogState.Status.INFO))

                    val groupsRows = getGroupsRows(groupsSheet)
                    progress(Excel.LogState("Acquired groups rows", Excel.LogState.Status.INFO))

                    val users = getUsersWithPosts(groupsRows)
                    progress(Excel.LogState("Acquired users posts", Excel.LogState.Status.INFO))

                    val postsRights = getPostsRights(groupsRows, rightsSheet)
                    progress(Excel.LogState("Acquired posts rights", Excel.LogState.Status.INFO))

                    val sortedUsers = sortUsersByRights(users, postsRights)
                    progress(Excel.LogState("Sorted users by rights", Excel.LogState.Status.INFO))

                    progress(Excel.LogState("Generating users ...", Excel.LogState.Status.INFO))
                    generateRightsByUsers(sortedUsers, postsRights, usersSheet, wb)

                    wb.write(FileOutputStream(toPath))
                    wb.close()
                    progress(
                        Excel.LogState(
                            "\nFinished generating users, file at $toPath\n",
                            Excel.LogState.Status.INFO
                        )
                    )
                } catch (e: Throwable) {
                    progress(Excel.LogState(e.localizedMessage, Excel.LogState.Status.ERROR))
                    e.printStackTrace()
                }
            }
    }

    companion object {
        const val EXCEL_FILE = "GE-TE-007"


        private const val USERS_SHEET = "Utilisateurs"
        private const val RIGHTS_SHEET = "Droits"
        private const val GROUPS_SHEET = "Groupes"

        private const val USERS_SHEET_FIRST_USER_ROW = 2
        private const val USERS_SHEET_FIRST_RIGHT_COLUMN = 1

        private const val RIGHTS_SHEET_FIRST_RIGHT_ROW = 2
        private const val RIGHTS_SHEET_FIRST_RIGHT_COLUMN = 2

        // Dictionary containing the value and color for each right
        private val RIGHTS_VALUE = mapOf(
            "--" to mapOf(
                "value" to 1,
                "color" to IndexedColors.WHITE1.index
            ),
            "r" to mapOf(
                "value" to 2,
                "color" to IndexedColors.DARK_RED.index
            ),
            "rw" to mapOf(
                "value" to 3,
                "color" to IndexedColors.LIGHT_GREEN.index
            )
        )
    }
}