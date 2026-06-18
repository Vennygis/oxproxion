package io.github.stardomains3.oxproxion
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class AssistantActivity : AppCompatActivity() {

    private var stateSnapshot: AssistantStateSnapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm: ChatViewModel by viewModels()
        val repository = PresetRepository(this)
        val allPresets = repository.getAll()

        // 1. Identify which preset the user is requesting
        val digitalAssistantPreset = allPresets.find {
            it.title.lowercase().trim() == "digital assistant"
        }
        val transcriptionPreset = allPresets.find {
            it.title.lowercase().trim() == "transcription"
        }

        // 2. Routing Logic
        // Priority 1: Digital Assistant (if it exists, we stay in this Activity)
        if (digitalAssistantPreset != null) {
            setupAssistantMode(vm, digitalAssistantPreset)
        }
        // Priority 2: Transcription (if Digital Assistant isn't found, but Transcription is)
        else if (transcriptionPreset != null) {
            launchTranscriptionActivity()
        }
        // Fallback: If nothing matches, just load the standard chat
        else {
            setupStandardChatMode(vm)
        }
    }

    /**
     * Logic for the standard Digital Assistant mode
     */
    private fun setupAssistantMode(vm: ChatViewModel, preset: Preset) {
        setContentView(R.layout.activity_main)

        // Capture state for restoration later
        stateSnapshot = PresetManager.captureCurrentState(this, vm)

        // Apply the preset
        PresetManager.applyPreset(this, vm, preset)
        vm.signalPresetApplied()

        // Load the standard ChatFragment
        loadChatFragment()
    }

    /**
     * Logic for the standard Chat mode (no presets)
     */
    private fun setupStandardChatMode(vm: ChatViewModel) {
        setContentView(R.layout.activity_main)
        loadChatFragment()
    }

    /**
     * Redirects the user to the specialized Transcription Activity
     */
    private fun launchTranscriptionActivity() {
        val intent = Intent(this, Transactivity::class.java)
        // We use startActivity and finish so the user doesn't "go back" into the assistant
        startActivity(intent)
        finish()
    }

    private fun loadChatFragment() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val chatFragment = ChatFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("start_stt_on_launch", true)
                }
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, chatFragment, "ChatFragment")
                .commitNow()
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Restore original state when assistant is destroyed
        stateSnapshot?.let { snapshot ->
            val vm: ChatViewModel by viewModels()
            PresetManager.restoreState(this, vm, snapshot)
        }
    }
}