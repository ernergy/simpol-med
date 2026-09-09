package com.simple.medai.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.medai.data.StudyQuestion
import com.simple.medai.data.StudyQuiz
import com.simple.medai.data.StudyRepository
import kotlinx.coroutines.launch

@Composable
fun StudyModeScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var topic by remember { mutableStateOf("") }
    var quiz by remember { mutableStateOf<StudyQuiz?>(null) }
    var currentIndex by remember { mutableStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var answered by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var round by remember { mutableStateOf(1) }

    val allAskedQuestions = remember {
        mutableStateListOf<String>()
    }

    BackHandler(enabled = true) {
        if (!loading) {
            onBack()
        }
    }

    fun loadQuestions(
        continuePractice: Boolean
    ) {
        val cleanTopic = topic.trim()

        if (cleanTopic.isBlank()) {
            errorText =
                "Escribe el tema que quieres practicar."
            return
        }

        loading = true
        errorText = ""

        scope.launch {
            try {
                val generated =
                    StudyRepository.generateQuiz(
                        topic = cleanTopic,
                        previousQuestions =
                            if (continuePractice) {
                                allAskedQuestions.toList()
                            } else {
                                emptyList()
                            }
                    )

                generated.questions
                    .forEach {
                        allAskedQuestions += it.question
                    }

                quiz = generated
                currentIndex = 0
                selectedIndex = null
                answered = false
                correctCount = 0

                if (continuePractice) {
                    round += 1
                } else {
                    round = 1
                }

                scrollState.animateScrollTo(0)

            } catch (e: Exception) {
                errorText =
                    e.message
                        ?: "No se pudo generar la práctica."
            } finally {
                loading = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color =
            MaterialTheme
                .colorScheme
                .background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(18.dp)
        ) {

            SimpleBackButton(
                onClick = {
                    if (!loading) {
                        onBack()
                    }
                },
                label = "INICIO"
            )

            Spacer(
                Modifier.height(14.dp)
            )

            SimpleHeader(
                title = "MODO ESTUDIO",
                subtitle =
                    "10 preguntas por ronda • práctica ilimitada"
            )

            Spacer(
                Modifier.height(18.dp)
            )

            if (quiz == null) {

                StudyStartCard(
                    topic = topic,
                    onTopicChange = {
                        topic = it
                        errorText = ""
                    },
                    loading = loading,
                    onStart = {
                        allAskedQuestions.clear()
                        loadQuestions(
                            continuePractice = false
                        )
                    }
                )

                if (
                    errorText.isNotBlank()
                ) {
                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        errorText,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }

            } else {

                val activeQuiz =
                    quiz!!

                val question =
                    activeQuiz
                        .questions
                        .getOrNull(
                            currentIndex
                        )

                if (question != null) {

                    StudyProgress(
                        title =
                            activeQuiz.title,
                        round = round,
                        current =
                            currentIndex + 1,
                        total =
                            activeQuiz
                                .questions
                                .size,
                        score =
                            correctCount
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    QuestionCard(
                        question =
                            question,
                        selectedIndex =
                            selectedIndex,
                        answered =
                            answered,
                        onSelect = { index ->
                            if (!answered) {
                                selectedIndex =
                                    index
                            }
                        }
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    if (!answered) {

                        Button(
                            onClick = {
                                val selection =
                                    selectedIndex

                                if (
                                    selection ==
                                    null
                                ) {
                                    errorText =
                                        "Selecciona una respuesta."
                                    return@Button
                                }

                                answered =
                                    true

                                errorText =
                                    ""

                                if (
                                    selection ==
                                    question
                                        .correctIndex
                                ) {
                                    correctCount +=
                                        1
                                }
                            },
                            enabled =
                                selectedIndex != null,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            shape =
                                SimpleButtonShape
                        ) {
                            Text(
                                "COMPROBAR RESPUESTA",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                    } else {

                        FeedbackCard(
                            isCorrect =
                                selectedIndex ==
                                    question
                                        .correctIndex,
                            correctAnswer =
                                question.options[
                                    question
                                        .correctIndex
                                ],
                            explanation =
                                question
                                    .explanation
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Button(
                            onClick = {

                                if (
                                    currentIndex <
                                    activeQuiz
                                        .questions
                                        .lastIndex
                                ) {
                                    currentIndex +=
                                        1

                                    selectedIndex =
                                        null

                                    answered =
                                        false

                                    errorText =
                                        ""

                                } else {

                                    currentIndex =
                                        activeQuiz
                                            .questions
                                            .size
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            shape =
                                SimpleButtonShape
                        ) {
                            Text(
                                if (
                                    currentIndex <
                                    activeQuiz
                                        .questions
                                        .lastIndex
                                ) {
                                    "SIGUIENTE PREGUNTA ➜"
                                } else {
                                    "VER RESULTADO"
                                },
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    if (
                        errorText
                            .isNotBlank()
                    ) {
                        Spacer(
                            Modifier.height(10.dp)
                        )

                        Text(
                            errorText,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }

                } else {

                    ResultsCard(
                        topic = topic,
                        score =
                            correctCount,
                        total =
                            activeQuiz
                                .questions
                                .size,
                        round =
                            round,
                        loading =
                            loading,
                        onContinue = {
                            loadQuestions(
                                continuePractice = true
                            )
                        },
                        onNewTopic = {
                            quiz = null
                            topic = ""
                            currentIndex = 0
                            correctCount = 0
                            selectedIndex = null
                            answered = false
                            round = 1
                            allAskedQuestions
                                .clear()
                            errorText = ""
                        }
                    )

                    if (
                        errorText
                            .isNotBlank()
                    ) {
                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            errorText,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun StudyStartCard(
    topic: String,
    onTopicChange: (String) -> Unit,
    loading: Boolean,
    onStart: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape = SimpleCardShape
    ) {

        Column(
            Modifier.padding(20.dp)
        ) {

            Text(
                "🎓",
                fontSize = 40.sp
            )

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                "¿Qué quieres practicar?",
                fontSize = 22.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                "Escribe cualquier tema. SIMPLE preparará 10 preguntas con cuatro opciones y explicación.",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(
                Modifier.height(16.dp)
            )

            OutlinedTextField(
                value = topic,
                onValueChange =
                    onTopicChange,
                modifier =
                    Modifier.fillMaxWidth(),
                enabled = !loading,
                placeholder = {
                    Text(
                        "Ej.: Anatomía del corazón"
                    )
                },
                shape =
                    SimpleButtonShape
            )

            Spacer(
                Modifier.height(14.dp)
            )

            Button(
                onClick =
                    onStart,
                enabled =
                    !loading &&
                        topic
                            .isNotBlank(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                shape =
                    SimpleButtonShape
            ) {

                if (loading) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .size(20.dp),
                        strokeWidth =
                            2.dp
                    )

                    Spacer(
                        Modifier.width(10.dp)
                    )
                }

                Text(
                    if (loading) {
                        "PREPARANDO PREGUNTAS…"
                    } else {
                        "EMPEZAR PRÁCTICA"
                    },
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StudyProgress(
    title: String,
    round: Int,
    current: Int,
    total: Int,
    score: Int
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            SimpleCardShape,
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                )
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Text(
                title,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement
                        .SpaceBetween
            ) {

                Text(
                    "Ronda $round"
                )

                Text(
                    "Pregunta $current/$total"
                )

                Text(
                    "✓ $score"
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            LinearProgressIndicator(
                progress = {
                    current
                        .coerceAtMost(total)
                        .toFloat() /
                        total
                            .coerceAtLeast(1)
                            .toFloat()
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: StudyQuestion,
    selectedIndex: Int?,
    answered: Boolean,
    onSelect: (Int) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            SimpleCardShape
    ) {

        Column(
            Modifier.padding(18.dp)
        ) {

            Text(
                question.question,
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(16.dp)
            )

            question.options
                .forEachIndexed {
                        index,
                        option ->

                    val isSelected =
                        selectedIndex ==
                            index

                    val isCorrect =
                        answered &&
                            index ==
                                question
                                    .correctIndex

                    val isWrong =
                        answered &&
                            isSelected &&
                            !isCorrect

                    OutlinedButton(
                        onClick = {
                            onSelect(index)
                        },
                        enabled =
                            !answered,
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        shape =
                            SimpleButtonShape
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                when {
                                    isCorrect ->
                                        "✓"
                                    isWrong ->
                                        "✕"
                                    isSelected ->
                                        "●"
                                    else ->
                                        "○"
                                },
                                fontSize =
                                    18.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Text(
                                option,
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(8.dp)
                    )
                }
        }
    }
}

@Composable
private fun FeedbackCard(
    isCorrect: Boolean,
    correctAnswer: String,
    explanation: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            SimpleCardShape,
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                )
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Text(
                if (isCorrect) {
                    "✓ Correcto"
                } else {
                    "✕ Respuesta incorrecta"
                },
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    17.sp
            )

            if (!isCorrect) {

                Spacer(
                    Modifier.height(6.dp)
                )

                Text(
                    "Respuesta correcta: $correctAnswer",
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                explanation,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultsCard(
    topic: String,
    score: Int,
    total: Int,
    round: Int,
    loading: Boolean,
    onContinue: () -> Unit,
    onNewTopic: () -> Unit
) {

    val percentage =
        if (total > 0) {
            score * 100 / total
        } else {
            0
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            SimpleCardShape
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                "🏁",
                fontSize = 42.sp
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                "Ronda $round terminada",
                fontSize = 22.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                topic,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                Modifier.height(18.dp)
            )

            Text(
                "$score / $total",
                fontSize = 42.sp,
                fontWeight =
                    FontWeight.Black
            )

            Text(
                "$percentage% de respuestas correctas",
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                Modifier.height(20.dp)
            )

            Button(
                onClick =
                    onContinue,
                enabled =
                    !loading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                shape =
                    SimpleButtonShape
            ) {

                if (loading) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .size(20.dp),
                        strokeWidth =
                            2.dp
                    )

                    Spacer(
                        Modifier.width(10.dp)
                    )
                }

                Text(
                    if (loading) {
                        "CREANDO 10 MÁS…"
                    } else {
                        "SIGAMOS PRACTICANDO"
                    },
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick =
                    onNewTopic,
                enabled =
                    !loading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                shape =
                    SimpleButtonShape
            ) {

                Text(
                    "CAMBIAR DE TEMA"
                )
            }
        }
    }
}
