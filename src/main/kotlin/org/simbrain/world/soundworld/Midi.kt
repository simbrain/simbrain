package org.simbrain.world.soundworld

import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer
import javax.swing.*
import javax.swing.event.ChangeEvent

fun main() {
    val frame = JFrame("MIDI Sound Generator")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(500, 200)

    val panel = JPanel(FlowLayout())

    // Create a synthesizer and open it
    val synthesizer: Synthesizer = MidiSystem.getSynthesizer()
    synthesizer.open()
    val midiChannels: Array<MidiChannel> = synthesizer.channels

    // Instrument dropdown
    val instruments = synthesizer.availableInstruments
    val instrumentNames = instruments.map { it.name }.toTypedArray()
    val instrumentDropdown = JComboBox(instrumentNames)
    panel.add(JLabel("Instrument:"))
    panel.add(instrumentDropdown)

    // Note dropdown
    val notes = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )
    val noteDropdown = JComboBox(notes)
    panel.add(JLabel("Note:"))
    panel.add(noteDropdown)

    // Volume slider
    val volumeSlider = JSlider(0, 127, 60)
    volumeSlider.addChangeListener { event: ChangeEvent? ->
        val slider = event?.source as? JSlider
        midiChannels[0].controlChange(71, slider?.value ?: 60) // 7 is the controller number for volume
    }
    panel.add(JLabel("Volume:"))
    panel.add(volumeSlider)

    // Play and Stop buttons
    val playButton = JButton("Play Note")
    playButton.addActionListener {
        val selectedInstrument = instrumentDropdown.selectedIndex
        val selectedNote = noteDropdown.selectedIndex + 60 // 60 is middle C
        synthesizer.loadInstrument(instruments[selectedInstrument])
        midiChannels[0].programChange(selectedInstrument)
        midiChannels[0].noteOn(selectedNote, volumeSlider.value)
    }
    panel.add(playButton)

    val stopButton = JButton("Stop Note")
    stopButton.addActionListener {
        midiChannels[0].noteOff(noteDropdown.selectedIndex + 60) // 60 is middle C
    }
    panel.add(stopButton)

    frame.add(panel, BorderLayout.CENTER)
    frame.isVisible = true
}