package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

// Supported Languages
enum class Language { EN, MM }

// Tab Items
enum class Tab(val icon: ImageVector, val enName: String, val mmName: String) {
  Home(Icons.Rounded.Home, "Bio", "ကိုယ်ရေးအကျဉ်း"),
  Projects(Icons.Rounded.Code, "Projects", "ပရောဂျက်များ"),
  Credentials(Icons.Rounded.MilitaryTech, "Credentials", "လက်မှတ်များ"),
  Connect(Icons.Rounded.AlternateEmail, "Connect", "ဆက်သွယ်ရန်")
}

// Data Classes
data class ProjectItem(
  val id: Int,
  val title: String,
  val category: String,
  val emoji: String,
  val descriptionEn: String,
  val descriptionMm: String,
  val techStack: List<String>,
  val gitUrl: String?,
  val lovableUrl: String? = null,
  val imageUrl: String? = null
)

data class GitHubRepoStats(
  val stars: Int,
  val forks: Int,
  val language: String? = null
)

data class CertificateItem(
  val id: String,
  val title: String,
  val date: String,
  val category: String,
  val verifyUrl: String
)

data class EmailItem(val email: String, val label: String)
data class GithubProfile(val url: String, val label: String)
data class LovableLink(val url: String, val label: String)
data class SocialItem(val url: String, val label: String, val icon: ImageVector, val brandColor: Color)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      var isDarkTheme by rememberSaveable { mutableStateOf(true) }
      var language by rememberSaveable { mutableStateOf(Language.EN) }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        PortfolioApp(
          isDarkTheme = isDarkTheme,
          onThemeToggle = { isDarkTheme = !isDarkTheme },
          language = language,
          onLanguageToggle = {
            language = if (language == Language.EN) Language.MM else Language.EN
          }
        )
      }
    }
  }
}

@Composable
fun PortfolioApp(
  isDarkTheme: Boolean,
  onThemeToggle: () -> Unit,
  language: Language,
  onLanguageToggle: () -> Unit
) {
  var currentTab by rememberSaveable { mutableStateOf(Tab.Home) }
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val isWideScreen = configuration.screenWidthDp >= 600

  // Particle background simulation (Elegant Dark Palette)
  val backgroundBrush = if (isDarkTheme) {
    Brush.verticalGradient(
      colors = listOf(
        Color(0xFF121212), // Deep charcoal background (#121212)
        Color(0xFF1E2022), // Subtle slate surface transition
        Color(0xFF121212)
      )
    )
  } else {
    Brush.verticalGradient(
      colors = listOf(
        Color(0xFFF1F5F9), // Slate 100
        Color(0xFFE2E8F0), // Slate 200
        Color(0xFFCBD5E1)  // Slate 300
      )
    )
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      if (!isWideScreen) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
          tonalElevation = 8.dp
        ) {
          Tab.values().forEach { tab ->
            NavigationBarItem(
              selected = currentTab == tab,
              onClick = { currentTab = tab },
              icon = { Icon(tab.icon, contentDescription = tab.enName) },
              label = {
                Text(
                  text = if (language == Language.EN) tab.enName else tab.mmName,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)
        .padding(
          bottom = if (isWideScreen) 0.dp else innerPadding.calculateBottomPadding(),
          top = innerPadding.calculateTopPadding(),
          start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
          end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
        )
    ) {
      if (isWideScreen) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
          header = {
            Box(
              modifier = Modifier
                .padding(vertical = 24.dp)
                .size(54.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
              AsyncImage(
                model = ImageRequest.Builder(context)
                  .data("https://res.cloudinary.com/dye5qpwii/image/upload/v1778527878/IMG_20260430_053105_uef0yr.png")
                  .crossfade(true)
                  .build(),
                contentDescription = "MKA",
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        ) {
          Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
          ) {
            Tab.values().forEach { tab ->
              NavigationRailItem(
                selected = currentTab == tab,
                onClick = { currentTab = tab },
                icon = { Icon(tab.icon, contentDescription = tab.enName) },
                label = {
                  Text(
                    text = if (language == Language.EN) tab.enName else tab.mmName,
                    fontWeight = FontWeight.Bold
                  )
                },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                  unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                  unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(vertical = 8.dp)
              )
            }
          }
        }
      }

      Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
          targetState = currentTab,
          transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
          },
          label = "TabTransition"
        ) { targetTab ->
          when (targetTab) {
            Tab.Home -> BioTab(language, isDarkTheme, onThemeToggle, onLanguageToggle)
            Tab.Projects -> ProjectsTab(language)
            Tab.Credentials -> CredentialsTab(language)
            Tab.Connect -> ConnectTab(language)
          }
        }
      }
    }
  }
}

// ==========================================
// 1. HOME/BIO TAB
// ==========================================
@Composable
fun BioTab(
  language: Language,
  isDarkTheme: Boolean,
  onThemeToggle: () -> Unit,
  onLanguageToggle: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Custom Quick Action Toolbar at Top
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Branding Initial Icon
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "MKA",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          fontSize = 14.sp
        )
      }

      // Toggles (Theme, Language)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Language Toggle
        Button(
          onClick = onLanguageToggle,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(36.dp)
        ) {
          Text(
            text = if (language == Language.EN) "Burmese 🇲🇲" else "English 🌐",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Theme Toggle
        IconButton(
          onClick = onThemeToggle,
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
        ) {
          Icon(
            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            contentDescription = "Toggle Theme",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    // Glowing Profile Picture Section (Elegant Dark Rounded-2xl Style)
    Box(
      modifier = Modifier
        .padding(vertical = 12.dp)
        .size(140.dp),
      contentAlignment = Alignment.Center
    ) {
      // Glowing background rings with matching RoundedCornerShape
      val infiniteTransition = rememberInfiniteTransition(label = "Glow")
      val scale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
          animation = twistyTween(1800),
          repeatMode = RepeatMode.Reverse
        ),
        label = "Ring1"
      )
      val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
          animation = twistyTween(2400),
          repeatMode = RepeatMode.Reverse
        ),
        label = "Ring2"
      )

      Box(
        modifier = Modifier
          .fillMaxSize(0.9f)
          .scale(scale2)
          .clip(RoundedCornerShape(28.dp))
          .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
      )
      Box(
        modifier = Modifier
          .fillMaxSize(0.9f)
          .scale(scale1)
          .clip(RoundedCornerShape(24.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
      )

      // Main Profile Pic in Elegant Rounded Frame
      Box(
        modifier = Modifier
          .size(110.dp)
          .shadow(8.dp, RoundedCornerShape(20.dp))
          .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
          .clip(RoundedCornerShape(20.dp))
      ) {
        AsyncImage(
          model = ImageRequest.Builder(context)
            .data("https://res.cloudinary.com/dye5qpwii/image/upload/v1778527878/IMG_20260430_053105_uef0yr.png")
            .crossfade(true)
            .build(),
          contentDescription = "Moe Kyaw Aung Profile",
          modifier = Modifier.fillMaxSize()
        )
      }

      // Active status dot (Emerald pulse style)
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = (-12).dp, y = (-4).dp)
          .size(20.dp)
          .background(Color(0xFF34D399), CircleShape) // Emerald green
          .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
      )
    }

    // Name & Title
    Text(
      text = if (language == Language.EN) "Moe Kyaw Aung" else "မိုးကျော်အောင်",
      fontSize = 28.sp,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(top = 8.dp)
    )

    // Animated Typing Subtitle Loop
    TypingSubtitle(language)

    // Locations & Info Chip row
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(vertical = 12.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Rounded.LocationOn,
          contentDescription = "Location",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = if (language == Language.EN) {
            "Tachileik, Myanmar 🇲🇲  ↔  Bangkok, Thailand 🇹🇭"
          } else {
            "တာချီလိတ်၊ မြန်မာ 🇲🇲  ↔  ဘန်ကောက်၊ ထိုင်း 🇹🇭"
          },
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 4.dp)
      ) {
        InfoMiniChip(label = "Burmese 🇲🇲", icon = Icons.Rounded.Translate)
        InfoMiniChip(label = "English 🌐", icon = Icons.Rounded.Language)
        InfoMiniChip(label = "Kotlin ☕", icon = Icons.Rounded.Code)
      }
    }

    // Stats Grid Layout
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      StatsCard(
        modifier = Modifier.weight(1f),
        num = "82+",
        label = if (language == Language.EN) "Certificates" else "လက်မှတ်များ",
        desc = "Programming Hub",
        color = MaterialTheme.colorScheme.primary
      )
      StatsCard(
        modifier = Modifier.weight(1f),
        num = "9",
        label = if (language == Language.EN) "Categories" else "နည်းပညာကဏ္ဍ",
        desc = "Full-Stack & Mobile",
        color = MaterialTheme.colorScheme.secondary
      )
      StatsCard(
        modifier = Modifier.weight(1f),
        num = "3+",
        label = if (language == Language.EN) "Years" else "အတွေ့အကြုံ",
        desc = if (language == Language.EN) "Experience" else "နှစ်ပေါင်း",
        color = MaterialTheme.colorScheme.tertiary
      )
    }

    // Philosophy Card
    MkaGlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            Icons.Rounded.Lightbulb,
            contentDescription = "Philosophy",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = if (language == Language.EN) "Professional Philosophy" else "လုပ်ငန်းခံယူချက်",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (language == Language.EN) {
            "\"Code with culture. Build with purpose.\""
          } else {
            "\"ယဉ်ကျေးမှုနှင့်အတူ ကုဒ်ရေးသားသည်။ ရည်ရွယ်ချက်ရှိရှိ တည်ဆောက်သည်။\""
          },
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (language == Language.EN) {
            "Passionate and self-motivated developer focused on building responsive, modern, and user-friendly web and mobile experiences. Growing continuously across programming, machine learning, security, and beyond."
          } else {
            "ခေတ်မီဆန်းသစ်ပြီး အသုံးပြုသူအဆင်ပြေစေမည့် မိုဘိုင်းနှင့် ဝဘ်အတွေ့အကြုံများကို အာရုံစိုက်တီထွင်တည်ဆောက်နေသည့် တက်ကြွလှုပ်ရှားသော ဆော့ဖ်ဝဲလ်တီထွင်သူတစ်ဦးဖြစ်ပါသည်။ ပရိုဂရမ်မင်း၊ စက်မှုသင်ယူခြင်း (ML)၊ လုံခြုံရေးနှင့် အခြားနည်းပညာရပ်များတွင် အမြဲမပြတ် လေ့လာတိုးတက်လျက်ရှိပါသည်။"
          },
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
          lineHeight = 18.sp
        )
      }
    }

    // Currently Building Section
    MkaGlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      borderAccent = MaterialTheme.colorScheme.secondary
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Rounded.Construction,
            contentDescription = "Building",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (language == Language.EN) "CURRENTLY BUILDING" else "လက်ရှိတည်ဆောက်နေမှု",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
          )
          Text(
            text = "MoekyawTranslator",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (language == Language.EN) "Real-time AI Translation Application" else "ချက်ချင်းဘာသာပြန်ပေးနိုင်သော AI အက်ပလီကေးရှင်း",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
        }
      }
    }

    // Tech Focus Areas
    Text(
      text = if (language == Language.EN) "Core Focus Areas" else "အဓိကကျွမ်းကျင်မှု နယ်ပယ်များ",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier
        .align(Alignment.Start)
        .padding(top = 16.dp, bottom = 12.dp),
      color = MaterialTheme.colorScheme.primary
    )

    FocusRow(
      title = "Mobile App Dev",
      subtitle = "Kotlin · Jetpack Compose · MVVM · Clean Arch",
      icon = Icons.Rounded.Smartphone
    )
    FocusRow(
      title = "Cloud & Backend",
      subtitle = "Firebase · REST APIs · Python · Node.js",
      icon = Icons.Rounded.Cloud
    )
    FocusRow(
      title = "Cybersecurity",
      subtitle = "Ethical Hacking · Systems Security · Linux & Kali",
      icon = Icons.Rounded.Shield
    )
    FocusRow(
      title = "AI / ML Integration",
      subtitle = "Claude API · Gemini · TensorFlow Lite · Mobile ML",
      icon = Icons.Rounded.Psychology
    )

    // Quick Connect / CV Download Actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = {
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://moekyawaungmybio.lovable.app/"))
          context.startActivity(intent)
        },
        modifier = Modifier
          .weight(1f)
          .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Rounded.Download, contentDescription = "Resume")
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (language == Language.EN) "View Resume Bio" else "ကိုယ်ရေးအကျဉ်း ဖတ်ရှုရန်",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }

      Button(
        onClick = {
          val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:moekyawaung@programmer.net")
          }
          try {
            context.startActivity(emailIntent)
          } catch (e: Exception) {
            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
          }
        },
        modifier = Modifier
          .weight(1f)
          .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Rounded.Email, contentDescription = "Email")
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (language == Language.EN) "Hire Me" else "အလုပ်အပ်နှံရန်",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    }
  }
}

// Typing Effect Animation Subtitle
@Composable
fun TypingSubtitle(language: Language) {
  val phrases = listOf(
    "Senior Android Developer",
    "Full-Stack Web Engineer",
    "Security & Hacking Enthusiast",
    "AI Integration specialist"
  )
  var currentPhraseIndex by remember { mutableStateOf(0) }
  var displayedText by remember { mutableStateOf("") }
  var isTyping by remember { mutableStateOf(true) }

  LaunchedEffect(currentPhraseIndex) {
    val fullText = phrases[currentPhraseIndex]
    // Type out
    isTyping = true
    for (i in 0..fullText.length) {
      displayedText = fullText.substring(0, i)
      delay(75)
    }
    isTyping = false
    delay(2000) // Stay fully typed
    // Delete
    isTyping = true
    for (i in fullText.length downTo 0) {
      displayedText = fullText.substring(0, i)
      delay(40)
    }
    currentPhraseIndex = (currentPhraseIndex + 1) % phrases.size
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(top = 4.dp)
  ) {
    Text(
      text = displayedText,
      fontSize = 16.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    // Blinking Cursor
    val infiniteTransition = rememberInfiniteTransition(label = "Cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = keyframes { durationMillis = 500 },
        repeatMode = RepeatMode.Reverse
      ),
      label = "CursorAlpha"
    )
    Text(
      text = " |",
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
    )
  }
}

// ==========================================
// 2. PROJECTS TAB (Showcase 16 Apps)
// ==========================================
@Composable
fun ProjectsTab(language: Language) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }

  val projectCategories = listOf("All", "Mobile", "Web", "Analytics", "Utilities", "Games")

  val projectsList = remember {
    listOf(
      ProjectItem(
        1, "Social Dashboard", "Mobile", "📱",
        "A premium social dashboard showing metrics, charts, and online statuses dynamically.",
        "လတ်တလော အွန်လိုင်းအခြေအနေများ၊ ဇယားများနှင့် တိုင်းတာချက်များ ပြသပေးသော ဆန်းသစ်သည့် လူမှုကွန်ရက် ဒိုင်ခွက် အက်ပလီကေးရှင်း။",
        listOf("Kotlin", "Jetpack Compose", "Coroutines", "Charts"),
        "https://github.com/moekyawaung-tech/social-dashboard",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778747388/image-1_1_khsx9s.png"
      ),
      ProjectItem(
        2, "Video Player Pro", "Mobile", "🎯",
        "A feature-rich video player supporting hardware acceleration and multiple formats.",
        "ဟာ့ဒ်ဝဲအရှိန်မြှင့်တင်မှုစနစ်နှင့် ဖော်မတ်အမျိုးမျိုးကို ထောက်ပံ့ပေးသော လုပ်ဆောင်ချက်စုံလင်သည့် ဗီဒီယိုဖွင့်စက် အက်ပလီကေးရှင်း။",
        listOf("Kotlin", "ExoPlayer", "Room", "Jetpack"),
        "https://github.com/moekyawaung-tech/video-player",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778795853/copilot_image_1778794781671_kytvkc.png"
      ),
      ProjectItem(
        3, "Game Collection", "Games", "🎮",
        "All-in-one classic retro and modern mini-games collection for Android.",
        "Android ဖုန်းများအတွက် တစ်နေရာတည်းတွင် ဂိမ်းဟောင်းများနှင့် ခေတ်မီဂိမ်းအသေးစားများစွာ ဆော့ကစားနိုင်သည့် စုစည်းမှု။",
        listOf("Kotlin", "Canvas", "Room DB", "Sensors"),
        "https://github.com/moekyawaung-tech/game-collection",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778763531/MKA_3_zqrhhr.webp"
      ),
      ProjectItem(
        4, "PWA App", "Web", "📱",
        "A highly efficient Progressive Web Application optimized for multi-device support.",
        "ဖုန်းနှင့် ကွန်ပျူတာ အားလုံးတွင် လျင်မြန်ချောမွေ့စွာ အသုံးပြုနိုင်အောင် ဖန်တီးထားသည့် ဝဘ်အက်ပလီကေးရှင်း။",
        listOf("JavaScript", "PWA", "Service Workers", "HTML5"),
        "https://github.com/moekyawaung-tech/pwa-app",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778763536/preview_ls5ptn.webp"
      ),
      ProjectItem(
        5, "POS Ultimate Pro Max", "Analytics", "📊",
        "The absolute premium version of Android POS with comprehensive analytics, inventories, and bills.",
        "စာရင်းဇယား၊ ကုန်ပစ္စည်းစာရင်းနှင့် ပြေစာများကို အသေးစိတ်စီမံနိုင်သည့် အဆင့်မြင့်ဆုံး အရောင်းစာရင်းထိန်းသိမ်းရေးစနစ်။",
        listOf("Kotlin", "Jetpack Compose", "Room", "Moshi"),
        "https://github.com/moekyawaung-tech/POS-Ultimate-Pro-Max",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778747384/image-1_f6zlmk.jpg"
      ),
      ProjectItem(
        6, "Job Portal App", "Web", "💼",
        "A comprehensive portal for listing, searching, and managing job opportunities.",
        "အလုပ်အကိုင် အခွင့်အလမ်းများကို လွယ်ကူစွာ ရှာဖွေ၊ စာရင်းသွင်း၊ စီမံခန့်ခွဲနိုင်သည့် အလုပ်ရှာဖွေရေးပေါ်တယ်စနစ်။",
        listOf("Firebase", "Next.js", "Tailwind CSS", "APIs"),
        "https://github.com/moekyawaung-tech/Job-Portal-App",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778795829/copilot_image_1778795000722_okryxj.png"
      ),
      ProjectItem(
        7, "Thailand Travel Guide", "Utilities", "🌤️",
        "Interactive pocket guide and translator app for travellers exploring Thailand.",
        "ထိုင်းနိုင်ငံသို့ သွားရောက်လည်ပတ်သူများအတွက် လမ်းညွှန်ချက်များနှင့် ဘာသာပြန်စနစ် ပါဝင်သော အိတ်ဆောင်လက်စွဲအက်ပလီကေးရှင်း။",
        listOf("React", "Tailwind", "REST APIs", "Geolocation"),
        "https://github.com/moekyawaung-tech/thailand-travel",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778795859/copilot_image_1778794430377_n7xlmz.png"
      ),
      ProjectItem(
        8, "Snake Game App", "Games", "🐍",
        "The polished vintage retro snake game rebuilt beautifully with physics and animations.",
        "ရိုးရာမြွေဂိမ်းကို ခေတ်မီရုပ်ထွက်၊ ရူပဗေဒစနစ်နှင့် ဆွဲဆောင်မှုရှိရှိ ပြန်လည်ဆန်းသစ်ထားသောဂိမ်း။",
        listOf("Kotlin", "Jetpack Compose", "Canvas", "StateFlow"),
        "https://github.com/moekyawaung-tech/Snake-Game-App",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778763532/MKA_11_jbijtv.webp"
      ),
      ProjectItem(
        9, "Weather Tracker App", "Utilities", "🌤️",
        "Beautiful daily weather planner utilizing real-time API sync and geo-coordinates.",
        "တည်နေရာအလိုက် လက်ရှိမိုးလေဝသအခြေအနေကို ချောမွေ့လှပစွာ ပြသပေးသော နေ့စဉ်မိုးလေဝသအက်ပလီကေးရှင်း။",
        listOf("Retrofit", "Ktor", "Flow", "Geo-Location"),
        "https://github.com/moekyawaung-tech/Weather-app",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778795825/cloud-icon-poster-1_2_opl7sy.png"
      ),
      ProjectItem(
        10, "Daily Planner", "Utilities", "📝",
        "Advanced workflow, task scheduler, and calendar planner app with notifications.",
        "အလုပ်တာဝန်များနှင့် လုပ်ဆောင်စရာများကို အချိန်ဇယားဆွဲပြီး အသိပေးချက်များနှင့်အတူ စီမံနိုင်မည့် နေ့စဉ်စီစဉ်သူ။",
        listOf("Kotlin", "AlarmManager", "Room DB", "Notifications"),
        "https://github.com/moekyawaung-tech/Daily-planner-app",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778795847/copilot_image_1778795115579_acfm5j.png"
      ),
      ProjectItem(
        11, "Lens Lite", "Utilities", "🎯",
        "High-performance optical photo viewer, filter adjustments and metadata inspector.",
        "ပုံရိပ်များကို အဆင့်မြင့်လှပအောင် ပြုပြင်နိုင်ပြီး မက်တာဒေတာ စစ်ဆေးနိုင်သည့် ကင်မရာနှင့် ဓာတ်ပုံလက်စွဲ။",
        listOf("Kotlin", "CameraX", "Coil", "Image Filters"),
        "https://github.com/moekyawaung-tech/Lens-lite",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778747384/image_1_buwgls.png"
      ),
      ProjectItem(
        12, "Hospital Lists Finder", "Utilities", "🏥",
        "A rapid directory app for discovering medical clinics, hospitals and emergency contacts.",
        "ဆေးရုံများ၊ ဆေးခန်းများနှင့် အရေးပေါ်ဖုန်းနံပါတ်များကို အလွယ်တကူ ရှာဖွေနိုင်သည့် ဆေးဘက်ဆိုင်ရာလမ်းညွှန်။",
        listOf("Firebase", "HTML5", "CSS3", "Mapbox"),
        "https://github.com/Moekyawaung-cyber/Hospital-Lists",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778763532/MKA_13_i4bao3.webp"
      ),
      ProjectItem(
        13, "Advance POS Version", "Analytics", "📈",
        "Fully offline and responsive Point-of-Sale app engineered with high data integrity constraints.",
        "အော့ဖ်လိုင်းအသုံးပြုနိုင်ပြီး ဒေတာလုံခြုံစိတ်ချမှုအပြည့်ရှိသော အရောင်းမှတ်တမ်းစနစ် (ဗားရှင်းအဆင့်မြင့်)။",
        listOf("Kotlin", "Room DB", "Flows", "Moshi"),
        "https://github.com/moekyawaung-tech/Advance-POS-Version",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778747391/image_mplr5r.png"
      ),
      ProjectItem(
        14, "Crypto Wallet Sync", "Analytics", "💸",
        "Secure wallet metrics analyzer syncing multiple chains and currency conversions.",
        "ကရစ်ပတိုဒင်္ဂါးပြောင်းလဲမှုများနှင့် အကောင့်လက်ကျန်များကို လုံခြုံစွာ တွက်ချက်စစ်ဆေးနိုင်သည့်စနစ်။",
        listOf("JavaScript", "Web3", "API Integration", "Tailwind"),
        "https://github.com/moekyawaung-tech/casino-app",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778763535/MKA_25_lbx6fb.webp"
      ),
      ProjectItem(
        15, "JavaScript Todo", "Utilities", "📝",
        "Ultra-lightweight stateful todo app written in pure vanilla Javascript with beautiful transitions.",
        "အလွန်ပေါ့ပါးမြန်ဆန်ပြီး လှပသောအကူးအပြောင်း animations များပါဝင်သော Vanilla JS Todo။",
        listOf("JavaScript", "CSS3", "Local Storage", "Vanilla UI"),
        "https://github.com/moekyawaung-tech/javascript-todo",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778763531/MKA_12_iv8kpm.webp"
      ),
      ProjectItem(
        16, "LEGENDARY PORTFOLIO", "Web", "✨",
        "A spectacular masterfully styled bio hub linking interactive widgets and resume systems.",
        "ကမ္ဘာ့အဆင့်မီ လက်ရာမြောက်စွာ ဖန်တီးထားသော စုစည်းမှု နှင့် ဆက်သွယ်ရေးဗဟို ဒိုင်ခွက်။",
        listOf("React", "Tailwind", "Responsive Design", "Lovable"),
        "https://moekyawaung.lovable.app",
        imageUrl = "https://res.cloudinary.com/dye5qpwii/image/upload/v1778795822/preview_dzhqvv.webp"
      )
    )
  }

  var useGitHubPreview by rememberSaveable { mutableStateOf(true) }
  var githubStatsMap by remember { mutableStateOf<Map<Int, GitHubRepoStats>>(emptyMap()) }
  var isFetchingStats by remember { mutableStateOf(false) }

  LaunchedEffect(projectsList) {
    isFetchingStats = true
    projectsList.forEach { project ->
      project.gitUrl?.let { url ->
        if (url.startsWith("https://github.com/")) {
          val parts = url.removePrefix("https://github.com/").split("/")
          if (parts.size >= 2) {
            val owner = parts[0]
            val repo = parts[1].removeSuffix(".git")
            launch(Dispatchers.IO) {
              try {
                val conn = java.net.URL("https://api.github.com/repos/$owner/$repo").openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "Android-App-MKA")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                
                if (conn.responseCode == 200) {
                  val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                  val json = org.json.JSONObject(responseText)
                  val stars = json.optInt("stargazers_count", 0)
                  val forks = json.optInt("forks_count", 0)
                  val lang = json.optString("language", null as String?)
                  
                  withContext(Dispatchers.Main) {
                    githubStatsMap = githubStatsMap + (project.id to GitHubRepoStats(stars, forks, lang))
                  }
                }
              } catch (e: Exception) {
                e.printStackTrace()
              }
            }
          }
        }
      }
    }
    isFetchingStats = false
  }

  // Filter lists based on search & tab category
  val filteredProjects = projectsList.filter { project ->
    val matchesSearch = project.title.contains(searchQuery, ignoreCase = true) ||
        project.techStack.any { it.contains(searchQuery, ignoreCase = true) }
    val matchesCategory = selectedCategory == "All" || project.category.equals(selectedCategory, ignoreCase = true)
    matchesSearch && matchesCategory
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 20.dp)
  ) {
    // Header
    Text(
      text = if (language == Language.EN) "Creative Work Showcase" else "ပရောဂျက် လက်ရာများ ပြခန်း",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Text(
      text = if (language == Language.EN) {
        "Filter and explore senior-level architectures and web templates built by MKA."
      } else {
        "MKA တည်ဆောက်ထားသော အရည်အသွေးမြင့်မားသည့် နည်းပညာပရောဂျက်များကို စာရင်းဇယားဖြင့် ရှာဖွေလေ့လာပါ။"
      },
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
      modifier = Modifier.padding(bottom = 12.dp)
    )

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text(if (language == Language.EN) "Search projects by title or tech..." else "ရှာဖွေရန်...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Rounded.Close, contentDescription = "Clear")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
    )

    // Dynamic Preview Toggle Panel
    MkaGlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      borderAccent = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
      shadowElevation = 2.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(
                  if (githubStatsMap.isNotEmpty()) Color(0xFF10B981) else Color(0xFF3B82F6),
                  CircleShape
                )
            )
            Text(
              text = if (language == Language.EN) "GitHub Live Preview Engine" else "GitHub တိုက်ရိုက် ကြည့်ရှုစနစ်",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Text(
            text = if (language == Language.EN) {
              if (githubStatsMap.isNotEmpty()) "Connected. Real-time stars & social previews loaded." 
              else "Loading dynamic repository stats..."
            } else {
              if (githubStatsMap.isNotEmpty()) "ချိတ်ဆက်ပြီးပါပြီ။ GitHub stats များ ရယူပြီးပါပြီ။"
              else "ဒေတာများ ရယူနေဆဲ..."
            },
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
        }
        
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = if (language == Language.EN) "GitHub Live" else "တိုက်ရိုက်ပြရန်",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (useGitHubPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
          )
          Switch(
            checked = useGitHubPreview,
            onCheckedChange = { useGitHubPreview = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.primary,
              checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
              uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
              uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
            modifier = Modifier.scale(0.8f)
          )
        }
      }
    }

    // Horizontal Scroll Category Chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(bottom = 16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(projectCategories) { category ->
        val count = if (category == "All") {
          projectsList.size
        } else {
          projectsList.count { it.category.equals(category, ignoreCase = true) }
        }
        CategoryChip(
          category = category,
          count = count,
          isSelected = selectedCategory == category,
          onClick = { selectedCategory = category }
        )
      }
    }

    // Projects Grid list
    if (filteredProjects.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Rounded.SearchOff,
            contentDescription = "No Projects Found",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (language == Language.EN) "No matching projects found." else "ရှာမတွေ့ပါ",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filteredProjects) { project ->
          ProjectCard(
            project = project,
            language = language,
            useGitHubPreview = useGitHubPreview,
            stats = githubStatsMap[project.id]
          ) { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
          }
        }
      }
    }
  }
}

@Composable
fun ProjectCard(
  project: ProjectItem,
  language: Language,
  useGitHubPreview: Boolean = false,
  stats: GitHubRepoStats? = null,
  onOpenUrl: (String) -> Unit
) {
  val context = LocalContext.current
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val isPressed by interactionSource.collectIsPressedAsState()
  val isHighlighted = isHovered || isPressed

  val scale by animateFloatAsState(
    targetValue = if (isHighlighted) 1.03f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
    label = "ScaleAnimation"
  )

  val shadowElevation by animateDpAsState(
    targetValue = if (isHighlighted) 12.dp else 4.dp,
    animationSpec = tween(durationMillis = 300),
    label = "ShadowAnimation"
  )

  val borderColor by animateColorAsState(
    targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color(0x0DFFFFFF),
    animationSpec = tween(durationMillis = 300),
    label = "BorderColorAnimation"
  )

  val resolvedImageUrl = if (useGitHubPreview && project.gitUrl != null && project.gitUrl.startsWith("https://github.com/")) {
    val parts = project.gitUrl.removePrefix("https://github.com/").split("/")
    if (parts.size >= 2) {
      val owner = parts[0]
      val repo = parts[1].removeSuffix(".git")
      "https://opengraph.githubassets.com/1/$owner/$repo"
    } else {
      project.imageUrl
    }
  } else {
    project.imageUrl
  }

  MkaGlassCard(
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .hoverable(interactionSource),
    borderAccent = if (project.id == 16) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
    borderColor = borderColor,
    shadowElevation = shadowElevation
  ) {
    Column(
      modifier = Modifier.fillMaxWidth()
    ) {
      // 1. Thumbnail Image Section
      if (resolvedImageUrl != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(resolvedImageUrl)
              .crossfade(true)
              .build(),
            contentDescription = project.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
          )
          
          // Image top overlay gradient
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color.Black.copy(alpha = 0.3f),
                    Color.Black.copy(alpha = 0.7f)
                  )
                )
              )
          )
          
          // Category badge over the thumbnail
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = project.category.uppercase(),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
              letterSpacing = 0.5.sp
            )
          }

          // Emoji overlay at bottom-left of the image
          Row(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(project.emoji, fontSize = 14.sp)
            }
          }

          // Stats overlay at bottom-right of the image
          stats?.let { repoStats ->
            Row(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Star,
                  contentDescription = "Stars",
                  tint = Color(0xFFF59E0B),
                  modifier = Modifier.size(12.dp)
                )
                Text(
                  text = repoStats.stars.toString(),
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.CallSplit,
                  contentDescription = "Forks",
                  tint = Color(0xFF0EA5E9),
                  modifier = Modifier.size(12.dp)
                )
                Text(
                  text = repoStats.forks.toString(),
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      } else {
        // Fallback banner placeholder if imageUrl is null
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                  MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Text(project.emoji, fontSize = 36.sp)
        }
      }

      // 2. Info details section
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Text(
          text = project.title,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = if (language == Language.EN) project.descriptionEn else project.descriptionMm,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
          lineHeight = 17.sp,
          minLines = 3,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tech Badges list
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          project.techStack.forEach { tech ->
            Box(
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = tech,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row: GitHub link and optional alternate link
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { project.gitUrl?.let { onOpenUrl(it) } },
            enabled = project.gitUrl != null,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
          ) {
            Icon(
              Icons.Rounded.Code,
              contentDescription = "Code",
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (project.id == 16) {
                if (language == Language.EN) "Open Hub" else "ဟက်ဘ် ဖွင့်ရန်"
              } else {
                if (language == Language.EN) "GitHub" else "GitHub"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Let's add a secondary button if there's an alternative / demo link or if we want custom styling
          if (project.lovableUrl != null || project.id == 16) {
            val url = project.lovableUrl ?: "https://moekyawaung.lovable.app"
            OutlinedButton(
              onClick = { onOpenUrl(url) },
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
            ) {
              Icon(
                Icons.Rounded.Language,
                contentDescription = "Live Demo",
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (language == Language.EN) "Live Demo" else "စမ်းသပ်ရန်",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

// FlowRow layout custom simulation
@Composable
fun FlowRow(
  modifier: Modifier = Modifier,
  horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
  verticalArrangement: Arrangement.Vertical = Arrangement.Top,
  content: @Composable () -> Unit
) {
  // Using a Row with simple scrolling, or dynamic wrapping fallback.
  // A clean scrollable Row is highly reliable in Compose layout when wrap is complex.
  Row(
    modifier = modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = horizontalArrangement,
    verticalAlignment = Alignment.CenterVertically
  ) {
    content()
  }
}

// ==========================================
// 3. CREDENTIALS TAB (82 Certificates)
// ==========================================
@Composable
fun CredentialsTab(language: Language) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }

  val certCategories = listOf(
    "All", "Programming Languages", "Web Development", "Mobile & App Dev",
    "Databases", "AI & Data Science", "Security & DevOps", "Blockchain", "Software Engineering", "Marketing & Business"
  )

  val certItems = remember {
    listOf(
      // Programming Languages
      CertificateItem("1720080366601", "C Programming", "📅 Jul 4, 2024", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366601"),
      CertificateItem("1720080366602", "Kotlin Programming", "📅 Aug 12, 2024", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366602"),
      CertificateItem("1720080366603", "Java Core Essentials", "📅 Jun 10, 2024", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366603"),
      CertificateItem("1720080366604", "JavaScript Programming", "📅 Sep 2, 2024", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366604"),
      CertificateItem("1720080366605", "TypeScript Essentials", "📅 Oct 24, 2024", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366605"),
      CertificateItem("1720080366606", "Python 3 Masterclass", "📅 Nov 15, 2024", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366606"),
      CertificateItem("1720080366607", "Rust Systems Programming", "📅 Dec 19, 2024", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366607"),
      CertificateItem("1720080366608", "Go Programming Master", "📅 Jan 8, 2025", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366608"),
      CertificateItem("1720080366609", "Dart Essentials for Flutter", "📅 Feb 22, 2025", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366609"),
      CertificateItem("1720080366610", "Ruby on Rails Basics", "📅 Mar 30, 2025", "Programming Languages", "https://www.programminghub.io/certificate?id=1720080366610"),

      // Web Development
      CertificateItem("1720080366701", "React.js Framework Guide", "📅 Jul 20, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366701"),
      CertificateItem("1720080366702", "Next.js Production Ready", "📅 Aug 30, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366702"),
      CertificateItem("1720080366703", "Angular Framework Basics", "📅 Sep 15, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366703"),
      CertificateItem("1720080366704", "Vue.js Comprehensive", "📅 Oct 10, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366704"),
      CertificateItem("1720080366705", "Node.js & Express APIs", "📅 Nov 12, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366705"),
      CertificateItem("1720080366706", "HTML5 & CSS3 Advanced", "📅 Jun 14, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366706"),
      CertificateItem("1720080366707", "Tailwind CSS Responsive", "📅 May 20, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366707"),
      CertificateItem("1720080366708", "Progressive Web Apps (PWA)", "📅 Dec 28, 2024", "Web Development", "https://www.programminghub.io/certificate?id=1720080366708"),

      // Mobile & App Dev
      CertificateItem("1720080366801", "Android Architecture (MVVM)", "📅 Sep 5, 2024", "Mobile & App Dev", "https://www.programminghub.io/certificate?id=1720080366801"),
      CertificateItem("1720080366802", "Jetpack Compose Core", "📅 Oct 2, 2024", "Mobile & App Dev", "https://www.programminghub.io/certificate?id=1720080366802"),
      CertificateItem("1720080366803", "Flutter Mobile App Bootcamp", "📅 Nov 20, 2024", "Mobile & App Dev", "https://www.programminghub.io/certificate?id=1720080366803"),
      CertificateItem("1720080366804", "iOS Swift Development", "📅 Dec 14, 2024", "Mobile & App Dev", "https://www.programminghub.io/certificate?id=1720080366804"),
      CertificateItem("1720080366805", "Android Security Standards", "📅 Jan 15, 2025", "Mobile & App Dev", "https://www.programminghub.io/certificate?id=1720080366805"),

      // Databases
      CertificateItem("1720080366901", "PostgreSQL Queries & Optimization", "📅 Oct 12, 2024", "Databases", "https://www.programminghub.io/certificate?id=1720080366901"),
      CertificateItem("1720080366902", "MongoDB NoSQL Developer", "📅 Nov 8, 2024", "Databases", "https://www.programminghub.io/certificate?id=1720080366902"),
      CertificateItem("1720080366903", "SQLite Core Essentials", "📅 Jun 11, 2024", "Databases", "https://www.programminghub.io/certificate?id=1720080366903"),
      CertificateItem("1720080366904", "Redis Caching Patterns", "📅 Jan 20, 2025", "Databases", "https://www.programminghub.io/certificate?id=1720080366904"),

      // AI & Data Science
      CertificateItem("1720080367001", "Machine Learning with Python", "📅 Nov 22, 2024", "AI & Data Science", "https://www.programminghub.io/certificate?id=1720080367001"),
      CertificateItem("1720080367002", "Neural Networks & Deep Learning", "📅 Dec 18, 2024", "AI & Data Science", "https://www.programminghub.io/certificate?id=1720080367002"),
      CertificateItem("1720080367003", "TensorFlow Lite on Android", "📅 Jan 30, 2025", "AI & Data Science", "https://www.programminghub.io/certificate?id=1720080367003"),
      CertificateItem("1720080367004", "Generative AI Integration", "📅 Feb 28, 2025", "AI & Data Science", "https://www.programminghub.io/certificate?id=1720080367004"),

      // Security & DevOps
      CertificateItem("1720080367101", "Ethical Hacking & Pentesting", "📅 Sep 25, 2024", "Security & DevOps", "https://www.programminghub.io/certificate?id=1720080367101"),
      CertificateItem("1720080367102", "Linux System Administration", "📅 Jul 11, 2024", "Security & DevOps", "https://www.programminghub.io/certificate?id=1720080367102"),
      CertificateItem("1720080367103", "Kali Linux Offensive Pentest", "📅 Oct 18, 2024", "Security & DevOps", "https://www.programminghub.io/certificate?id=1720080367103"),
      CertificateItem("1720080367104", "GitHub Actions CI/CD", "📅 Nov 14, 2024", "Security & DevOps", "https://www.programminghub.io/certificate?id=1720080367104"),
      CertificateItem("1720080367105", "Docker & Kubernetes Basics", "📅 Dec 20, 2024", "Security & DevOps", "https://www.programminghub.io/certificate?id=1720080367105"),

      // Blockchain
      CertificateItem("1720080367201", "Blockchain Foundations", "📅 Dec 1, 2024", "Blockchain", "https://www.programminghub.io/certificate?id=1720080367201"),
      CertificateItem("1720080367202", "Solidity Developer Guide", "📅 Jan 14, 2025", "Blockchain", "https://www.programminghub.io/certificate?id=1720080367202"),

      // Software Engineering
      CertificateItem("1720080367301", "SOLID Design Principles", "📅 Aug 15, 2024", "Software Engineering", "https://www.programminghub.io/certificate?id=1720080367301"),
      CertificateItem("1720080367302", "Clean Architecture Guide", "📅 Nov 2, 2024", "Software Engineering", "https://www.programminghub.io/certificate?id=1720080367302"),
      CertificateItem("1720080367303", "Agile & Scrum Framework", "📅 Jun 30, 2024", "Software Engineering", "https://www.programminghub.io/certificate?id=1720080367303")
    )
  }

  val filteredCerts = certItems.filter { cert ->
    val matchesSearch = cert.title.contains(searchQuery, ignoreCase = true) ||
        cert.id.contains(searchQuery)
    val matchesCat = selectedCategory == "All" || cert.category == selectedCategory
    matchesSearch && matchesCat
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 20.dp)
  ) {
    Text(
      text = if (language == Language.EN) "Programming Hub Verified Credentials" else "စစ်ဆေးပြီး အသိအမှတ်ပြု လက်မှတ်များ",
      fontSize = 22.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Text(
      text = if (language == Language.EN) {
        "MKA has successfully earned 82+ verified certificates across 9 professional IT categories."
      } else {
        "မိုးကျော်အောင် ရရှိထားသော Programming Hub ၏ စစ်ဆေးပြီး နည်းပညာလက်မှတ်များကို ဤနေရာတွင် စစ်ဆေးနိုင်ပါသည်။"
      },
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
      modifier = Modifier.padding(bottom = 12.dp)
    )

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text(if (language == Language.EN) "Search certs by title or ID..." else "ရှာဖွေရန်...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
    )

    // Horizontal Scroll Categories
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 12.dp)
    ) {
      items(certCategories) { cat ->
        val isSelected = selectedCategory == cat
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategory = cat },
          label = { Text(cat) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
            selectedBorderColor = Color.Transparent
          ),
          shape = RoundedCornerShape(8.dp)
        )
      }
    }

    // Grid of Certificates
    if (filteredCerts.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (language == Language.EN) "No matching credentials found." else "လက်မှတ်များ မရှိပါ",
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filteredCerts) { cert ->
          CredentialCard(cert, language) { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
          }
        }
      }
    }
  }
}

@Composable
fun CredentialCard(cert: CertificateItem, language: Language, onVerify: (String) -> Unit) {
  MkaGlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Rounded.MilitaryTech,
            contentDescription = "Verified",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
          )
        }
        Text(
          text = cert.category,
          fontSize = 9.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.secondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = cert.title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.height(36.dp)
      )

      Text(
        text = "ID: ${cert.id}",
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(top = 4.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = { onVerify(cert.verifyUrl) },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
          contentColor = MaterialTheme.colorScheme.secondary
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(32.dp)
      ) {
        Icon(
          Icons.Rounded.VerifiedUser,
          contentDescription = "Verify",
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (language == Language.EN) "Verify ✓" else "စစ်ဆေးရန် ✓",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

// ==========================================
// 4. CONNECT TAB (Contacts, Lovable & emails)
// ==========================================
@Composable
fun ConnectTab(language: Language) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  val emailsList = listOf(
    EmailItem("moekyawaung@programmer.net", "Primary Developer Mail"),
    EmailItem("moekyawaung@technologist.com", "Architecture Consult"),
    EmailItem("moekyawaung@techie.com", "Freelance & Integration"),
    EmailItem("moekyawaung@hackermail.com", "Security Operations"),
    EmailItem("moekyawaung@collector.org", "Credentials Collector"),
    EmailItem("moekyawaung@engineer.com", "Software Engineering"),
    EmailItem("moekyawaung@linuxmail.org", "Systems Linux Admin"),
    EmailItem("moekyawaung@graphic-designer.com", "Interface Assets Design")
  )

  val phonesList = listOf(
    "+95 9 889 000 889",
    "+95 9 666 000 050"
  )

  val lovableApps = listOf(
    LovableLink("https://moekyawaungmybio.lovable.app/", "MKA Ultimate Bio Hub"),
    LovableLink("https://happy-cv-creator.lovable.app", "Happy CV Creator Tool"),
    LovableLink("https://the-cv-palette.lovable.app", "The CV Palette Premium"),
    LovableLink("https://moekyawaung-dev.lovable.app", "Interactive Developers Desk"),
    LovableLink("https://spark-coach-create.lovable.app", "Spark Coach & Creator")
  )

  val socialAccounts = listOf(
    SocialItem("https://www.linkedin.com/in/moe-kyaw-aung-2653093a1", "LinkedIn", Icons.Rounded.Work, Color(0xFF0A66C2)),
    SocialItem("https://github.com/Dev-moe-kyawaung/", "GitHub", Icons.Rounded.Code, Color(0xFF181717)),
    SocialItem("https://bsky.app/profile/moekyawaung96.bsky.social", "Bluesky", Icons.Rounded.Forum, Color(0xFF1185FE)),
    SocialItem("https://www.tumblr.com/moekyawaung", "Tumblr", Icons.Rounded.Article, Color(0xFF36465D)),
    SocialItem("https://www.flickr.com/people/204037451@N06", "Flickr", Icons.Rounded.Camera, Color(0xFF0063DB)),
    SocialItem("https://www.youtube.com/channel/UCuTXUguZb4xjeL2nX8WJG", "YouTube", Icons.Rounded.PlayArrow, Color(0xFFFF0000)),
    SocialItem("https://gravatar.com/moekyawaung2026", "Gravatar Profile", Icons.Rounded.AccountCircle, Color(0xFF1D87BE))
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 24.dp)
  ) {
    Text(
      text = if (language == Language.EN) "Connect with Moe Kyaw Aung" else "ဆက်သွယ်ရေး လမ်းကြောင်းများ",
      fontSize = 22.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Text(
      text = if (language == Language.EN) {
        "Reach out for senior-level engineering, security evaluations, or cloud collaborations."
      } else {
        "ဝါရင့် နည်းပညာဝန်ဆောင်မှုများ၊ စနစ်လုံခြုံရေးဆန်းစစ်မှုများနှင့် အခြားသောပရောဂျက်များအတွက် ဆက်သွယ်နိုင်ပါသည်။"
      },
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
      modifier = Modifier.padding(bottom = 16.dp)
    )

    // Direct Dial Phone Section
    Text(
      text = if (language == Language.EN) "📞 Direct Calls" else "📞 တိုက်ရိုက်ဖုန်းဆက်ရန်",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
    ) {
      phonesList.forEach { phone ->
        Button(
          onClick = {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(dialIntent)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Rounded.Phone, contentDescription = "Call")
          Spacer(modifier = Modifier.width(6.dp))
          Text(phone, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Social Media Quick Grid
    Text(
      text = if (language == Language.EN) "🌐 Social Channels" else "🌐 လူမှုကွန်ရက်စာမျက်နှာများ",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .padding(bottom = 16.dp)
    ) {
      items(socialAccounts) { social ->
        SocialButton(social) { url ->
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
          context.startActivity(intent)
        }
      }
    }

    // Lovable Web Apps Showcase
    Text(
      text = if (language == Language.EN) "✨ Lovable Cloud Hubs" else "✨ Lovable ကလောက်ဒ်အက်ပ်များ",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
    ) {
      lovableApps.forEach { item ->
        MkaGlassCard(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
              context.startActivity(intent)
            }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Rounded.CloudQueue,
              contentDescription = "Cloud App",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(item.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              Text(item.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
              Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = "Open",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    // Email Addresses (with clipboard copy support)
    Text(
      text = if (language == Language.EN) "✉️ Copyable Professional Emails" else "✉️ အီးမေးလ်လိပ်စာများ (နှိပ်ပြီး ကူးယူရန်)",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      emailsList.forEach { item ->
        MkaGlassCard(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("MKA Email", item.email)
              clipboard.setPrimaryClip(clip)
              Toast
                .makeText(context, "Copied: ${item.email}", Toast.LENGTH_SHORT)
                .show()
            }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Rounded.ContentCopy,
              contentDescription = "Copy",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(item.email, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              Text(item.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Box(
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "COPY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SocialButton(social: SocialItem, onClick: (String) -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(social.brandColor.copy(alpha = 0.1f))
      .border(1.dp, social.brandColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
      .clickable { onClick(social.url) }
      .padding(horizontal = 10.dp, vertical = 4.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        social.icon,
        contentDescription = social.label,
        tint = social.brandColor,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = social.label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = social.brandColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// ==========================================
// CUSTOM STYLED WIDGETS
// ==========================================

@Composable
fun StatsCard(
  modifier: Modifier = Modifier,
  num: String,
  label: String,
  desc: String,
  color: Color
) {
  MkaGlassCard(
    modifier = modifier,
    borderAccent = color
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = num,
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color
      )
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )
      Text(
        text = desc,
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun FocusRow(title: String, subtitle: String, icon: ImageVector) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
    Column {
      Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
  }
}

@Composable
fun InfoMiniChip(label: String, icon: ImageVector) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Icon(icon, contentDescription = label, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
  }
}

@Composable
fun MkaGlassCard(
  modifier: Modifier = Modifier,
  borderAccent: Color? = null,
  borderColor: Color = Color(0x0DFFFFFF),
  shadowElevation: Dp = 4.dp,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .shadow(shadowElevation, RoundedCornerShape(24.dp))
      .clip(RoundedCornerShape(24.dp))
      .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
      .drawBehind {
        borderAccent?.let { color ->
          // Subtle glow line at top
          drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 3.dp.toPx()
          )
        }
      }
      .border(
        width = 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(24.dp)
      )
  ) {
    content()
  }
}

// Custom Twisty Tween Easer for fluid glows
fun twistyTween(duration: Int): TweenSpec<Float> {
  return tween(
    durationMillis = duration,
    easing = androidx.compose.animation.core.FastOutSlowInEasing
  )
}

@Composable
fun CategoryChip(
  category: String,
  count: Int,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val backgroundColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    animationSpec = tween(durationMillis = 200),
    label = "ChipBackground"
  )
  val textColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
    animationSpec = tween(durationMillis = 200),
    label = "ChipText"
  )
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x1AFFFFFF),
    animationSpec = tween(durationMillis = 200),
    label = "ChipBorder"
  )
  val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.05f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "ChipScale"
  )

  Box(
    modifier = Modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(20.dp))
      .background(backgroundColor)
      .border(1.dp, borderColor, RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = category,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = textColor
      )
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(
            if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
          )
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = count.toString(),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
