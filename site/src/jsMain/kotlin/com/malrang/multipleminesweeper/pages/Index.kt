package com.malrang.multipleminesweeper.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.icons.fa.FaFaceSmile
import com.varabyte.kobweb.silk.components.icons.fa.FaFont
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


var BOARD_SIZE = 10
var MINE_COUNT = 15
var MINE_MULTIPLE = 1

@Page
@Composable
fun HomePage() {
    val screenWidth = window.innerWidth
    val screenHeight = window.innerHeight

    gamePC(screenWidth, screenHeight)
}


@Composable
fun gamePC(screenWidth : Int, screenHeight: Int) {
    var board by remember { mutableStateOf(generateBoard(MINE_MULTIPLE)) }
    var revealed by remember { mutableStateOf(List(BOARD_SIZE) { MutableList(BOARD_SIZE) { false } }) }
    var flagged by remember { mutableStateOf(List(BOARD_SIZE) { MutableList(BOARD_SIZE) { 0 } }) }
    var gameOver by remember { mutableStateOf(false) }
    var flagMode by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var tempBoardSize = remember { mutableStateOf(BOARD_SIZE) }
    var tempMineCount = remember { mutableStateOf(MINE_COUNT) }
    var tempMineMultiple = remember { mutableStateOf(MINE_MULTIPLE) }
    var boardSizePx by remember { mutableStateOf(screenWidth) }
    var boardSizePy by remember { mutableStateOf(screenHeight) }
    var timer by remember { mutableStateOf(0) }
    var remainingMines by remember { mutableStateOf(MINE_COUNT) }
    LaunchedEffect(gameOver) {
        while (!gameOver && timer < 999) {
            delay(1000L)
            timer++
        }
    }

    LaunchedEffect(Unit) {
        window.addEventListener("resize", {
            boardSizePx = window.innerWidth
            boardSizePy = window.innerHeight
        })
    }

    fun resetGame() {
        val newBoard = generateBoard(tempMineMultiple.value)
        remainingMines = newBoard.sumOf { row -> row.sumOf { if (it < 0) -it else 0 } }
        board = newBoard
        revealed = List(BOARD_SIZE) { MutableList(BOARD_SIZE) { false } }
        flagged = List(BOARD_SIZE) { MutableList(BOARD_SIZE) { 0 } }
        gameOver = false
        flagMode = false
        timer = 0
    }

    fun reveal(x: Int, y: Int) {
        if (gameOver || revealed[x][y] || flagged[x][y] > 0) return
        revealed = revealed.mapIndexed { i, row ->
            row.mapIndexed { j, cell ->
                if (i == x && j == y) true else cell
            }.toMutableList()
        }

        if (board[x][y] < 0) {
            gameOver = true

            // 모든 지뢰 칸 공개
            revealed = revealed.mapIndexed { i, row ->
                row.mapIndexed { j, _ ->
                    revealed[i][j] || board[i][j] < 0
                }.toMutableList()
            }

        } else if (board[x][y] == 0) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until BOARD_SIZE && ny in 0 until BOARD_SIZE && !revealed[nx][ny]) {
                        reveal(nx, ny)
                    }
                }
            }
        }
    }

    fun toggleFlag(x: Int, y: Int) {
        if (revealed[x][y]) return // 이미 열린 칸은 깃발 못 놓음

        flagged = flagged.mapIndexed { i, row ->
            row.mapIndexed { j, flagCount ->
                if (i == x && j == y) {
                    // 깃발 개수 순환: 0 → 1 → 2 → ... → 최대 → 0
                    val newFlagCount = (flagCount + 1) % (tempMineMultiple.value + 1)
                    remainingMines += flagCount - newFlagCount

                    newFlagCount
                } else flagCount
            }.toMutableList()
        }
    }



    Column(
        modifier = Modifier.fillMaxSize().padding(leftRight = 20.px)
        ,horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SpanText("Remain : $remainingMines")
            Button(onClick = { showSettings = true }) {
                FaFaceSmile()
            }
            Button(onClick = { flagMode = !flagMode }) {
                Text(if (flagMode) "🚩" else "💣")
            }
            Text("Time: $timer", )
            Box(
                Modifier.position(Position.Fixed)
                    .bottom(16.px)
                    .right(16.px)
                    .zIndex(1000) // 다른 UI 위로 올림
                    .styleModifier {
                        property("transform", "scale(1)") // 줌을 무시하고 항상 일정 크기 유지
                        property("transform-origin", "right bottom") // 오른쪽 아래를 기준으로 고정
                        property("touch-action", "none") // 터치 이벤트 방지 (필요하면 제거)
                    }
            ) {
                Button(onClick = { } ) {
                    Text("⚙")
                }
            }
        }
        SpanText("Board Size: ${BOARD_SIZE} x ${BOARD_SIZE}, Mines: $MINE_COUNT")

        val cellSize = minOf(boardSizePx / (BOARD_SIZE+2), boardSizePy / (BOARD_SIZE+1)).px

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            for (x in 0 until BOARD_SIZE) {
                Row {
                    for (y in 0 until BOARD_SIZE) {
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .border(1.px, LineStyle.Solid, Color.black)
                                .backgroundColor(
                                    when {
                                        flagged[x][y] > 0 -> Color.yellow
                                        gameOver && board[x][y] < 0 -> Color.red
                                        revealed[x][y] -> Color.lightgray
                                        else -> Color.darkgray
                                    },
//                                    shape = RoundedCornerShape(4.dp)
                                )
                                .onClick {
                                    if (flagMode) toggleFlag(x, y) else reveal(x, y)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (flagged[x][y] > 0) {
                                Text("🚩x${flagged[x][y]}")
                            }
                            else if (revealed[x][y]) {
                                Text(
//                                    color = Color.Black
                                    when {
                                        board[x][y] < 0 -> "💣x${-board[x][y]}"  // 한 칸에 여러 개의 지뢰가 있을 경우
                                        board[x][y] == 0 -> ""
                                        else -> board[x][y].toString()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (gameOver) {
            Text("Game Over! Press Restart to play again.")
        }
    }


    if (showSettings) {
        Box(
            modifier = Modifier.fillMaxSize()
                .backgroundColor(Color("#D3D3D399")) //r,g,b,a
            ,contentAlignment = Alignment.Center
        ){
            Box(
                modifier = Modifier
                    .backgroundColor(Color.white)
                    .padding(20.px)
                    .borderRadius(12.px)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SpanText(
                    "Game Settings",
                        Modifier.fontSize(30.px).fontWeight(FontWeight.Bold)
                    )
                    Text("Board Size (N*N): ${tempBoardSize.value}")
                    NumberSelector(tempBoardSize ,2, 20)

                    Text("Mines Count: ${tempMineCount.value.coerceAtMost(tempBoardSize.value * tempBoardSize.value - 1)}")
                    NumberSelector(tempMineCount ,1, tempBoardSize.value * tempBoardSize.value-1)

                    Text("Mines Mutiple: ${tempMineMultiple.value}")
                    NumberSelector(tempMineMultiple ,1, 5)


                    Row(
                        Modifier.margin(top = 20.px)
                    ){
                        Button(
                            onClick = {showSettings = false}
                        ){
                            Text("취소")
                        }
                        Button(
                            onClick = {
                                MINE_COUNT = tempMineCount.value.coerceAtMost(tempBoardSize.value * tempBoardSize.value - 1)
                                BOARD_SIZE = tempBoardSize.value
                                resetGame()
                                showSettings = false
                            }
                        ){
                            Text("확인")
                        }
                    }
                }
            }


        }
    }
}



fun generateBoard(mineMultiple : Int): Array<IntArray> {
    val board = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) }
    val mines = mutableMapOf<Pair<Int, Int>, Int>()
    while (mines.size < MINE_COUNT) {
        var x = Random.nextInt(BOARD_SIZE)
        var y = Random.nextInt(BOARD_SIZE)

        var isMine = mines[Pair(x,y)]?: 0
        while(isMine < 0){ //이미 지뢰가 있는 칸이면
            x = Random.nextInt(BOARD_SIZE)
            y = Random.nextInt(BOARD_SIZE)
            isMine = mines[Pair(x,y)]?: 0
        }
        mines[Pair(x, y)] = (Random.nextInt(mineMultiple) + 1)
    }


    for ((x, y) in mines.keys) {
        board[x][y] = -mines[Pair(x,y)]!!
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until BOARD_SIZE &&
                    ny in 0 until BOARD_SIZE &&
                    board[nx][ny] >= 0
                ) {
                    board[nx][ny] += mines[Pair(x,y)]!!
                }
            }
        }
    }


    return board
}



@Composable
fun NumberSelector(
    value: MutableState<Int>,
    min: Int = 2,
    max: Int = 20
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "-" 버튼
        Button(
            onClick = { value.value = max(min, value.value - 1) },
            Modifier.padding(8.px)
        ) {
            Text("<")
        }

        // 숫자 입력 필드
        Input(
            type = InputType.Number,
            attrs = {
                attr("min", "$min")
                attr("max", "$max")
                value(value.value.toString()) // 현재 값 표시
                onInput { event ->
                    val input = event.value?.toInt() ?: min // 숫자만 허용
                    value.value = input.coerceIn(min, max) // 범위 제한 적용
                }
            }
        )

        // "+" 버튼
        Button(
            onClick = { value.value = min(max, value.value + 1) },
            Modifier.padding(8.px)
        ) {
            Text(">")
        }
    }
}
