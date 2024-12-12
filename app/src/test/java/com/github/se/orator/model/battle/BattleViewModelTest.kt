package com.github.se.orator.model.battle

import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.profile.UserStatistics
import com.github.se.orator.model.speaking.InterviewContext
import com.github.se.orator.model.speechBattle.BattleRepository
import com.github.se.orator.model.speechBattle.BattleStatus
import com.github.se.orator.model.speechBattle.BattleViewModel
import com.github.se.orator.model.speechBattle.SpeechBattle
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.network.ChatGPTService
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class BattleViewModelTest {

  @Mock private lateinit var mockNavigationActions: NavigationActions
  @Mock private lateinit var mockBattleRepository: BattleRepository
  @Mock private lateinit var mockUserProfileRepository: UserProfileRepository
  @Mock private lateinit var chatGPTService: ChatGPTService

  private lateinit var userProfileViewModel: UserProfileViewModel
  private lateinit var battleViewModel: BattleViewModel
  private lateinit var apiLinkViewModel: ApiLinkViewModel
  private lateinit var chatViewModel: ChatViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    Dispatchers.setMain(testDispatcher)

    // Mock getCurrentUserUid to return "testUser"
    whenever(mockUserProfileRepository.getCurrentUserUid()).thenReturn("testUser")

    // Prepare a test UserProfile
    val testUserProfile =
        UserProfile(
            uid = "testUser",
            name = "Test User",
            age = 25,
            statistics = UserStatistics(),
            friends = listOf("friend1", "friend2"),
            bio = "Test bio")

    // Mock getUserProfile to invoke onSuccess with testUserProfile when called with "testUser"
    doAnswer { invocation ->
          val uid = invocation.getArgument<String>(0)
          val onSuccess = invocation.getArgument<(UserProfile?) -> Unit>(1)
          val onFailure = invocation.getArgument<(Exception) -> Unit>(2)
          if (uid == "testUser") {
            onSuccess.invoke(testUserProfile)
          } else {
            onFailure.invoke(Exception("User not found"))
          }
          null
        }
        .whenever(mockUserProfileRepository)
        .getUserProfile(any(), any(), any())

    // Initialize UserProfileViewModel with the mocked repository
    userProfileViewModel = UserProfileViewModel(mockUserProfileRepository)

    // Mock generateUniqueBattleId to return "testBattleId"
    whenever(mockBattleRepository.generateUniqueBattleId()).thenReturn("testBattleId")

    apiLinkViewModel = ApiLinkViewModel()

    // Initialize ChatViewModel with mocked dependencies
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    // Initialize BattleViewModel with all mocked and real dependencies
    battleViewModel =
        BattleViewModel(
            battleRepository = mockBattleRepository,
            userProfileViewModel = userProfileViewModel,
            navigationActions = mockNavigationActions,
            apiLinkViewModel = apiLinkViewModel,
            chatViewModel = chatViewModel)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `createBattleRequest should call storeBattleRequest`() = runTest {
    // Arrange
    val friendUid = "friendUid"
    val context =
        InterviewContext(
            targetPosition = "Software Engineer",
            companyName = "company",
            interviewType = "technical",
            experienceLevel = "junior",
            jobDescription = "Implement features",
            focusArea = "backend")

    val expectedBattle =
        SpeechBattle(
            battleId = "testBattleId",
            challenger = "testUser",
            opponent = friendUid,
            status = BattleStatus.PENDING,
            context = context,
            challengerCompleted = false,
            opponentCompleted = false,
            challengerData = emptyList(),
            opponentData = emptyList())

    // Act
    battleViewModel.createBattleRequest(friendUid, context)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    verify(mockBattleRepository).generateUniqueBattleId()
    verify(mockBattleRepository).storeBattleRequest(eq(expectedBattle), any())
  }

  @Test
  fun `listenForPendingBattles should update pendingBattles LiveData`() = runTest {
    // Arrange
    val testBattle1 =
        SpeechBattle(
            battleId = "battle1",
            challenger = "user1",
            opponent = "testUid",
            status = BattleStatus.PENDING,
            context =
                InterviewContext(
                    targetPosition = "",
                    companyName = "",
                    interviewType = "",
                    experienceLevel = "",
                    jobDescription = "",
                    focusArea = ""))

    val testBattle2 =
        SpeechBattle(
            battleId = "battle2",
            challenger = "user3",
            opponent = "testUid",
            status = BattleStatus.PENDING,
            context =
                InterviewContext(
                    targetPosition = "",
                    companyName = "",
                    interviewType = "",
                    experienceLevel = "",
                    jobDescription = "",
                    focusArea = ""))

    val pendingBattles = listOf(testBattle1, testBattle2)

    // Mock repository behavior
    whenever(mockBattleRepository.listenForPendingBattles(eq("testUser"), any())).thenAnswer {
        invocation ->
      val callback = invocation.getArgument<(List<SpeechBattle>) -> Unit>(1)
      callback.invoke(pendingBattles)
      null
    }

    // Act
    battleViewModel.listenForPendingBattles()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    Assert.assertEquals(pendingBattles, battleViewModel.pendingBattles.value)
  }

  @Test
  fun `acceptBattle should update battle status to IN_PROGRESS`() = runTest {
    // Arrange
    val battleId = "battleId"

    // Mock getBattleById to return a battle where testUser is the opponent
    whenever(mockBattleRepository.getBattleById(eq(battleId), any())).thenAnswer { invocation ->
      val callback = invocation.getArgument<(SpeechBattle?) -> Unit>(1)
      callback.invoke(
          SpeechBattle(
              battleId = battleId,
              challenger = "friendUid",
              opponent = "testUser",
              status = BattleStatus.PENDING,
              context =
                  InterviewContext(
                      targetPosition = "Dev",
                      companyName = "TestCorp",
                      interviewType = "hr",
                      experienceLevel = "entry",
                      jobDescription = "A job",
                      focusArea = "front-end")))
      null
    }

    // Mock updateBattleStatus to simulate success
    whenever(
            mockBattleRepository.updateBattleStatus(
                eq(battleId), eq(BattleStatus.IN_PROGRESS), any()))
        .thenAnswer { invocation ->
          val callback = invocation.getArgument<(Boolean) -> Unit>(2)
          callback.invoke(true) // Simulate success
          null
        }

    // Act
    battleViewModel.acceptBattle(battleId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    verify(mockBattleRepository)
        .updateBattleStatus(eq(battleId), eq(BattleStatus.IN_PROGRESS), any())
  }

  @Test
  fun `acceptBattle should not update battle status if getBattleById fails`() = runTest {
    // Arrange
    val battleId = "battleId"

    // Mock getBattleById to simulate failure
    whenever(mockBattleRepository.getBattleById(eq(battleId), any())).thenAnswer { invocation ->
      val callback = invocation.getArgument<(SpeechBattle?) -> Unit>(1)
      callback.invoke(null) // Simulate failure to fetch battle
      null
    }

    // Act
    battleViewModel.acceptBattle(battleId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    verify(mockBattleRepository).getBattleById(eq(battleId), any())
    verify(mockBattleRepository, never())
        .updateBattleStatus(eq(battleId), eq(BattleStatus.IN_PROGRESS), any())
  }

  @Test
  fun `acceptBattle should handle updateBattleStatus failure`() = runTest {
    // Arrange
    val battleId = "battleId"

    // Mock getBattleById to return a valid battle
    whenever(mockBattleRepository.getBattleById(eq(battleId), any())).thenAnswer { invocation ->
      val callback = invocation.getArgument<(SpeechBattle?) -> Unit>(1)
      callback.invoke(
          SpeechBattle(
              battleId = battleId,
              challenger = "friendUid",
              opponent = "testUser",
              status = BattleStatus.PENDING,
              context =
                  InterviewContext(
                      targetPosition = "Dev",
                      companyName = "TestCorp",
                      interviewType = "hr",
                      experienceLevel = "entry",
                      jobDescription = "A job",
                      focusArea = "front-end")))
      null
    }

    // Mock updateBattleStatus to simulate failure
    whenever(
            mockBattleRepository.updateBattleStatus(
                eq(battleId), eq(BattleStatus.IN_PROGRESS), any()))
        .thenAnswer { invocation ->
          val callback = invocation.getArgument<(Boolean) -> Unit>(2)
          callback.invoke(false) // Simulate failure
          null
        }

    // Act
    battleViewModel.acceptBattle(battleId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    verify(mockBattleRepository).getBattleById(eq(battleId), any())
    verify(mockBattleRepository)
        .updateBattleStatus(eq(battleId), eq(BattleStatus.IN_PROGRESS), any())
  }

  @Test
  fun `listenForPendingBattles should handle repository failure`() = runTest {
    // Arrange
    // Mock listenForPendingBattles to simulate failure by not invoking the callback
    whenever(mockBattleRepository.listenForPendingBattles(eq("testUser"), any())).thenAnswer {
        invocation ->
      val callback = invocation.getArgument<(List<SpeechBattle>) -> Unit>(1)
      // Simulate failure by invoking with empty list
      callback.invoke(emptyList())
      null
    }

    // Act
    battleViewModel.listenForPendingBattles()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    verify(mockBattleRepository).listenForPendingBattles(eq("testUser"), any())
    Assert.assertEquals(emptyList<SpeechBattle>(), battleViewModel.pendingBattles.value)
  }

  @Test
  fun `BattleViewModel should initialize correctly`() {
    // Assert
    Assert.assertNotNull(battleViewModel)
    Assert.assertNotNull(battleViewModel.pendingBattles)
  }

  @Test
  fun `declineBattle should call updateBattleStatus with CANCELLED`() = runTest {
    // Arrange
    val battleId = "testBattleId"

    // Mock the repository to simulate a successful update
    whenever(
            mockBattleRepository.updateBattleStatus(
                eq(battleId), eq(BattleStatus.CANCELLED), any()))
        .thenAnswer { invocation ->
          val callback = invocation.getArgument<(Boolean) -> Unit>(2)
          callback.invoke(true) // Simulate success
          null
        }

    // Act
    battleViewModel.declineBattle(battleId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    verify(mockBattleRepository).updateBattleStatus(eq(battleId), eq(BattleStatus.CANCELLED), any())
  }
}
