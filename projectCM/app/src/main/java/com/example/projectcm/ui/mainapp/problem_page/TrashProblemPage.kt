package com.example.projectcm.ui.mainapp.problem_page

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.projectcm.SharedViewModel
import com.example.projectcm.database.entities.TrashProblem
import com.example.projectcm.database.repositories.TrashProblemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TrashProblemViewModel(
    sharedViewModel: SharedViewModel, private val repository: TrashProblemRepository
) : ViewModel() {

    private val _sortByStatus = MutableStateFlow(true) 
    val sortByStatus: StateFlow<Boolean> = _sortByStatus

    private val _trashProblem = mutableStateOf<TrashProblem?>(null)

    val trashProblems = combine(
        repository.getAllTrashProblems(), _sortByStatus, sharedViewModel.currentUser
    ) { problems, sortByStatus, user ->
        val filteredProblems = if (user?.role == "User") {
            problems.filter { it.userId == user.id }
        } else {
            problems 
        }
        filteredProblems.sortedWith(
            compareBy({ if (sortByStatus) it.status else null },
                { it.reportedAt })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSortByStatus() {
        _sortByStatus.value = !_sortByStatus.value
    }

    fun getProblemById(problemId: Int): TrashProblem? {
        return trashProblems.value.find { it.id == problemId }
    }

    fun resolveProblem(problem: TrashProblem, adminName: String) {
        viewModelScope.launch {
            try {
                repository.updateTrashProblem(
                    problem.copy(
                        status = "Resolved", resolvedAt = LocalDateTime.now(), adminName = adminName
                    )
                )
            } catch (e: Exception) {
                Log.e("TrashProblemViewModel", "Error resolving problem", e)
            }
        }

    }

    fun setTrashProblem(trashProblem: TrashProblem) {
        _trashProblem.value = trashProblem
    }


    fun setPhotoUri(photoUri: Uri) {
        _trashProblem.value = _trashProblem.value?.copy(imagePath = photoUri.toString())
    }

    fun saveTrashProblem() {
        _trashProblem.value?.let {
            viewModelScope.launch {
                try {
                    repository.addTrashProblem(it)
                } catch (e: Exception) {
                    Log.e("TrashProblemViewModel", "Error saving problem", e)
                }
            }
        }

    }
}

@Composable
fun ProblemsPage(
    sharedViewModel: SharedViewModel,
    viewModel: TrashProblemViewModel,
    navController: NavController
) {
    val currentUser by sharedViewModel.currentUser.collectAsState()
    val problems by viewModel.trashProblems.collectAsState(emptyList())
    val sortByStatus by viewModel.sortByStatus.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        
        Text(
            text = if (currentUser?.role == "User") "My Problems" else "All Problems",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        
        Button(
            onClick = { viewModel.toggleSortByStatus() },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = if (sortByStatus) "Sort by Date" else "Sort by Status")
        }

        
        LazyColumn {
            items(items = problems, key = { it.id }) { problem ->
                ProblemListItem(problem = problem) {
                    navController.navigate("Problem_Details/${problem.id}")
                }
            }
        }
    }
}

@Composable
fun ProblemListItem(problem: TrashProblem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Problem ID: ${problem.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Status: ${problem.status}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            
            if (problem.imagePath.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = problem.imagePath),
                    contentDescription = "Problem Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
fun ProblemDetailsScreen(
    problemId: Int,
    sharedViewModel: SharedViewModel,
    viewModel: TrashProblemViewModel,
    navController: NavController
) {
    val currentUser by sharedViewModel.currentUser.collectAsState()
    val problem = viewModel.getProblemById(problemId) 

    problem?.let {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Id: ${problem.id}")
                
                if (currentUser?.role == "Admin") {
                    Text("Reported by User ID: ${problem.userId}")
                }
                Text("Status: ${problem.status}")
                Text("Reported At: ${problem.reportedAt}")
                problem.resolvedAt?.let {
                    Text("Resolved At: $it")
                }
                Text("Latitude: ${problem.latitude}")
                Text("Longitude: ${problem.longitude}")
                Text("Admin Responsible: ${problem.adminName ?: "None"}")

                if (currentUser?.role == "Admin" && it.status == "Reported") {
                    Button(
                        onClick = {
                            viewModel.resolveProblem(it, currentUser!!.username)
                            navController.navigate("problems")
                        }, modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text("Resolve")
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                
                if (problem.imagePath.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = problem.imagePath),
                        contentDescription = "Problem Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

