package com.animalcollector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.animalcollector.ai.MockAnimalRecognitionService
import com.animalcollector.data.*
import com.animalcollector.domain.*
import com.animalcollector.game.*
import com.animalcollector.ui.AnimalTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity: ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AnimalTheme { App() } } } }

@Composable private fun App() {
 val context=LocalContext.current; val db=remember { Room.databaseBuilder(context,AppDatabase::class.java,"animal-collector.db").addMigrations(AppDatabase.MIGRATION_1_2).build() }; val cards by db.cards().observeActive().collectAsState(initial=emptyList()); val scope=rememberCoroutineScope(); var page by remember { mutableStateOf("home") }; var pending by remember { mutableStateOf<Uri?>(null) }; var recognition by remember { mutableStateOf<AnimalRecognitionResult?>(null) }; var uploads by remember { mutableIntStateOf(0) }; var message by remember { mutableStateOf<String?>(null) }; val profileStore=remember { LocalProfileStore(context) }; val profileName by profileStore.name.collectAsState(initial = "Исследователь")
 LaunchedEffect(Unit) { val item=db.uploads().get(); uploads=if(item?.day==LocalDate.now().toString()) item.count else 0 }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri != null) { if(uploads >=2) message=context.getString(R.string.upload_limit) else { uploads++; scope.launch { db.uploads().save(DailyUploadEntity(day=LocalDate.now().toString(),count=uploads)); recognition=MockAnimalRecognitionService().recognize(uri); pending=uri; page="name" } } } }
 val cameraPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) message="Камера готова. В этой версии используйте системный выбор фото для безопасного сохранения снимка." else message="Разрешение камеры не предоставлено." }
 Scaffold(containerColor=MaterialTheme.colorScheme.background, snackbarHost={ SnackbarHost(remember { SnackbarHostState() }) }) { padding -> Box(Modifier.padding(padding)) { when(page) { "home" -> Home(uploads, { if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) picker.launch(ActivityResultContracts.PickVisualMedia.ImageOnly) else cameraPermission.launch(Manifest.permission.CAMERA) }, { picker.launch(ActivityResultContracts.PickVisualMedia.ImageOnly) }, {page="collection"}, {page="bag"}, {page="more"}); "name" -> NameCard(pending,recognition, { name -> val r=recognition ?: return@NameCard; val score=RarityScoreCalculator().calculate("${pending}${System.nanoTime()}",r); val state=if(BagManager().canStore(Rarity.fromScore(score),cards)) StorageState.IN_BAG else StorageState.OUT_OF_BAG; scope.launch { db.cards().insert(CardEntity(java.util.UUID.randomUUID().toString(),name,pending.toString(),r.species,r.scientificName,r.family.name,r.breed,r.color,r.confidence,r.description,r.wikipediaQuery,score,Rarity.fromScore(score).name,System.currentTimeMillis(),state.name)); page="collection" } }, {page="home"}); "collection" -> Collection(cards,{page="home"}); "bag" -> Bag(cards,{page="home"}); "settings" -> Settings(profileName, { name -> scope.launch { profileStore.saveName(name); message="Локальный профиль сохранён" } }, {page="home"}); else -> More({page="home"},{page="settings"}) }; message?.let { AlertDialog(onDismissRequest={message=null},confirmButton={TextButton({message=null}){Text("Понятно")}},title={Text("Animal Collector")},text={Text(it)}) } } }
}
@Composable private fun Home(uploads:Int,onCamera:()->Unit,onPick:()->Unit,onCollection:()->Unit,onBag:()->Unit,onMore:()->Unit) { Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) { Text("🐱  Animal Collector",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black); Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF18364A)),shape=RoundedCornerShape(24.dp)){ Column(Modifier.padding(18.dp)){Text("Исследователь • уровень 1"); LinearProgressIndicator(.18f,Modifier.fillMaxWidth().padding(top=8.dp)); Text("120 Points",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp)) } }; Button(onCamera,Modifier.fillMaxWidth().height(62.dp),shape=RoundedCornerShape(20.dp)){Icon(Icons.Default.PhotoCamera,null); Spacer(Modifier.width(10.dp));Text(stringResource(R.string.camera))}; OutlinedButton(onPick,Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(20.dp)){Icon(Icons.Default.AddAPhoto,null);Spacer(Modifier.width(10.dp));Text(stringResource(R.string.picker))}; Text("Загрузки сегодня: $uploads/2",style=MaterialTheme.typography.bodySmall); Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ SmallNav("Коллекция",Icons.Default.Collections,onCollection);SmallNav("Сумка",Icons.Default.Inventory,onBag);SmallNav("Ещё",Icons.Default.Star,onMore) } } }
@Composable private fun SmallNav(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,click:()->Unit){ ElevatedCard(Modifier.width(108.dp).height(92.dp).clickable(onClick=click)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(icon,label);Text(label)}} }
@Composable private fun NameCard(uri:Uri?,r:AnimalRecognitionResult?,create:(String)->Unit,back:()->Unit){ var name by remember{mutableStateOf("")}; Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Text("Результат распознавания",style=MaterialTheme.typography.headlineSmall);Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp)){Text(r?.species?:"Анализируем…",style=MaterialTheme.typography.titleLarge);Text("Порода: ${r?.breed?:"не определена"}");Text("Окрас: ${r?.color?:"не определён"}");Text("Уверенность AI: ${r?.confidence?:0}%");Text(r?.description?:"")}};Text("Как назвать это животное?");OutlinedTextField(name,{name=it},label={Text(stringResource(R.string.animal_name))},singleLine=true,modifier=Modifier.fillMaxWidth());Button({create(name.trim().ifBlank { r?.species?:"Находка" })},enabled=r!=null,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.create_card))};TextButton(back){Text("Назад")}} }
@Composable private fun Collection(cards:List<CardEntity>,back:()->Unit){ Column(Modifier.fillMaxSize().padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,"Назад")};Text("МОЯ КОЛЛЕКЦИЯ",style=MaterialTheme.typography.headlineSmall)}; if(cards.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Ваша коллекция ждёт первое открытие 🐾") } else LazyVerticalGrid(GridCells.Fixed(2),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){items(cards){ c->Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=rarityColor(c.rarity))){Column(Modifier.padding(14.dp).height(135.dp),verticalArrangement=Arrangement.SpaceBetween){Text(c.name,fontWeight=FontWeight.Bold);Text(c.species);Text("${c.rarity} • ${c.score}",fontWeight=FontWeight.Black);if(c.state=="OUT_OF_BAG")Text("⚠️ Вне сумки",color=Color(0xFFFFD166))}}} } } }
@Composable private fun Bag(cards:List<CardEntity>,back:()->Unit){ Column(Modifier.fillMaxSize().padding(20.dp)){TextButton(back){Text("← Назад")};Text("СУМКА",style=MaterialTheme.typography.headlineSmall);Rarity.entries.forEach { rarity -> val used=cards.count{it.rarity==rarity.name&&it.state=="IN_BAG"}; ListItem(headlineContent={Text(rarity.name)},supportingContent={Text("$used / 5 мест")},trailingContent={Text("${used*20}%")}) };Text("Карточки вне сумки не удаляются сразу: защитите их или купите слот.",modifier=Modifier.padding(top=16.dp)) } }
@Composable private fun More(back:()->Unit,settings:()->Unit){ Column(Modifier.fillMaxSize().padding(20.dp)){TextButton(back){Text("← Назад")};Text("Игровые разделы",style=MaterialTheme.typography.headlineSmall);listOf("Альбомы", "Достижения", "Исследовательский уровень", "Статистика", "Локальный обмен").forEach{ ListItem(headlineContent={Text(it)},supportingContent={Text("Функция находится в разработке.")}) }; Button(settings,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Settings,null); Spacer(Modifier.width(8.dp));Text("Настройки и резервная копия")}} }
@Composable private fun Settings(currentName:String,save:(String)->Unit,back:()->Unit){ var name by remember(currentName){mutableStateOf(currentName)}; Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){TextButton(back){Text("← Назад")};Text(stringResource(R.string.local_profile),style=MaterialTheme.typography.headlineSmall);Text(stringResource(R.string.local_storage_notice));OutlinedTextField(name,{name=it},label={Text(stringResource(R.string.profile_name))},singleLine=true,modifier=Modifier.fillMaxWidth());Button({save(name)},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.save_profile))};Text("Данные коллекции сохраняются в базе Room на устройстве. При удалении приложения Android удаляет и его локальные данные.",style=MaterialTheme.typography.bodySmall)} }
private fun rarityColor(r:String)=when(r){"UNCOMMON"->Color(0xFF236B4A);"RARE"->Color(0xFF244F8F);"EPIC"->Color(0xFF563A88);"LEGENDARY"->Color(0xFF806020);else->Color(0xFF394A57)}
