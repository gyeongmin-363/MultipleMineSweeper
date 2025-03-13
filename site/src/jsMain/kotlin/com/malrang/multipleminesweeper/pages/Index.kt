package com.malrang.multipleminesweeper.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
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


fun adjustFloatingButtonPosition() {
    val button = document.getElementById("floating-button") as? HTMLDivElement
    if (button != null) {
        val width = window.innerWidth
        val height = window.innerHeight

        // 항상 화면 오른쪽 아래에 위치하도록 조정
        button.style.position = "absolute"
        button.style.left = "${width - 80}px" // 버튼 크기 감안
        button.style.top = "${height - 80}px"
    }
}

// 화면 크기 변경 이벤트 리스너 추가
fun setupResizeListener() {
    window.addEventListener("resize", { adjustFloatingButtonPosition() })
    window.addEventListener("scroll", { adjustFloatingButtonPosition() }) // 스크롤 시에도 유지
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
    var gameClear by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    LaunchedEffect(gameOver, gameClear) {
        while (!gameOver && !gameClear && timer < 9999) {
            delay(1000L)
            timer++
        }
    }

    LaunchedEffect(Unit) {
        window.addEventListener("resize", {
            boardSizePx = window.innerWidth
            boardSizePy = window.innerHeight
        })

        setupResizeListener() // 화면 크기 변화 감지
        adjustFloatingButtonPosition() // 초기 위치 설정
    }

    fun resetGame() {
        val newBoard = generateBoard(tempMineMultiple.value)
        remainingMines = newBoard.sumOf { row -> row.sumOf { if (it < 0) -it else 0 } }
        board = newBoard
        revealed = List(BOARD_SIZE) { MutableList(BOARD_SIZE) { false } }
        flagged = List(BOARD_SIZE) { MutableList(BOARD_SIZE) { 0 } }
        gameOver = false
        gameClear = false
        flagMode = false
        timer = 0
    }

    fun checkGameClear() {
        val totalCells = BOARD_SIZE * BOARD_SIZE
        val revealedCells = revealed.sumOf { row -> row.count { it } }
        val mineCells = board.sumOf { row -> row.count { it < 0 } }

        if (revealedCells + mineCells == totalCells) {
            gameClear = true
            showClear = true
        }
    }

    fun reveal(x: Int, y: Int) {
        if (gameOver || gameClear || revealed[x][y] || flagged[x][y] > 0) return
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

        checkGameClear() // 게임 클리어 확인
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


    Box(
        modifier = Modifier.fillMaxSize().backgroundColor(Color.lightslategray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.margin(all = 10.px).padding(10.px)
                .backgroundColor(Color.lightgray)
                .border(5.px, LineStyle.Outset)
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpanText("🚩 $remainingMines", Modifier
                    .backgroundColor(Color.darkgray)
                    .padding(8.px)
                    .borderRadius(4.px)
                )

                Button(
                    onClick = { showSettings = true }
                    ,modifier = Modifier.backgroundColor(Color.dimgray)
                ) {
                    Text("⚙️")
                }
                Button(
                    onClick = { flagMode = !flagMode },
                    modifier = Modifier.backgroundColor(if (flagMode) Color("#ffd663") else Color.blueviolet)

                ) {
                    Text(if (flagMode) "🚩" else "💣")
                }
                SpanText("🕒 $timer",Modifier
                    .backgroundColor(Color.darkgray)
                    .padding(8.px)
                    .borderRadius(4.px)
                )
            }

            val cellSize = minOf(boardSizePx / (BOARD_SIZE + 1), boardSizePy / (BOARD_SIZE + 1)).px

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.border(5.px, LineStyle.Inset).margin(top = 20.px)
            ) {
                for (x in 0 until BOARD_SIZE) {
                    Row {
                        for (y in 0 until BOARD_SIZE) {
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .border(if(flagged[x][y] > 0) 3.px else if(!revealed[x][y]) 5.px else 1.px,
                                        if(!revealed[x][y]) LineStyle.Outset else LineStyle.Solid)
                                    .backgroundColor(
                                        when {
                                            gameOver && board[x][y] < 0 -> Color.red
                                            flagged[x][y] > 0 -> Color("#ffd663")
                                            revealed[x][y] -> Color.lightgray
                                            else -> Color.darkgray
                                        }
                                    )
                                    .onClick {
                                        if (flagMode) toggleFlag(x, y) else reveal(x, y)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (flagged[x][y] > 0) {
                                    SpanText("🚩x${flagged[x][y]}", Modifier.fontSize(if(screenWidth < 600) 12.px else 18.px))
                                } else if (revealed[x][y]) {
                                    SpanText(
                                        when {
                                            board[x][y] < 0 -> "💣x${-board[x][y]}"  // 한 칸에 여러 개의 지뢰가 있을 경우
                                            board[x][y] == 0 -> ""
                                            else -> board[x][y].toString()
                                        },
                                        Modifier.fontSize(if(screenWidth < 600) 12.px else 18.px)
                                            .fontWeight(FontWeight.Bold)
                                            .color(
                                                when(board[x][y] % 8){
                                                    0 -> Color.black
                                                    1 -> Color.blue
                                                    2 -> Color.green
                                                    3 -> Color.red
                                                    4 -> Color.darkblue
                                                    5 -> Color.brown
                                                    6 -> Color("#6bdb8b")
                                                    7 -> Color.purple
                                                    else -> Color.black
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (gameOver) {
                SpanText("Game Over! Press Setting to play again."
                    , modifier = Modifier.color(Color.red)
                )
            }
            if (gameClear) {
                SpanText("⭐Game Clear⭐"
                    , modifier = Modifier.color(Color.yellow)
                )
            }
        }

        if(showClear){
            Box(
                modifier = Modifier.fillMaxSize()
                    .backgroundColor(Color("#D3D3D399")) //r,g,b,a
                , contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .backgroundColor(Color("#ebebeb"))
                        .padding(20.px)
                        .borderRadius(12.px)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpanText(
                            "⭐Clear⭐",
                            Modifier.fontSize(30.px).fontWeight(FontWeight.Bold)
                        )
                        SpanText("Board Size (N*N) : ${tempBoardSize.value}",
                            Modifier.margin(top = 5.px))

                        SpanText("Mines Count : ${tempMineCount.value}"
                            ,Modifier.margin(top = 5.px))

                        SpanText("Mines Multiple : ${tempMineMultiple.value}",
                            Modifier.margin(top = 5.px))

                        SpanText("time : $timer",
                            Modifier.margin(top = 5.px))


                        Button(
                            onClick = {
                                showClear = false
                            },
                            modifier = Modifier.margin(top = 20.px)
                        ) {
                            Text("✅")
                        }
                    }
                }
            }
        }


        if (showSettings) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .backgroundColor(Color("#D3D3D399")) //r,g,b,a
                , contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .backgroundColor(Color("#ebebeb"))
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
                        SpanText("Board Size (N*N)",
                            Modifier.margin(top = 5.px))
                        NumberSelector(tempBoardSize, 2, 20)

                        SpanText("Mines Count"
                            ,Modifier.margin(top = 5.px))
                        NumberSelector(tempMineCount, 1, tempBoardSize.value * tempBoardSize.value - 1)

                        SpanText("Mines Multiple",
                            Modifier.margin(top = 5.px))
                        NumberSelector(tempMineMultiple, 1, 5)


                        Row(
                            Modifier.margin(top = 20.px).fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Button(
                                onClick = { showSettings = false }
                            ) {
                                Text("❌")
                            }
                            Button(
                                onClick = {
                                    MINE_COUNT =
                                        tempMineCount.value.coerceAtMost(tempBoardSize.value * tempBoardSize.value - 1)
                                    BOARD_SIZE = tempBoardSize.value
                                    resetGame()
                                    showSettings = false
                                }
                            ) {
                                Text("✅")
                            }
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

        if(value.value > max){
            value.value = max
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
            },
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
