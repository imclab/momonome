package momonome;

import java.util.ArrayList;

import momonome.OscMonome.MonomeEventListener;
import momonome.util.Metronome;
import momonome.util.Metronome.MetronomeListener;
import oscP5.OscP5;


public class MonomeSequencer extends OscMonome implements MonomeEventListener, MetronomeListener
{
	public Metronome metronome;
	public int position;
	public int patternIndex;
	
	private int[][][] patterns;
	private int[] playHeadSlice;
	
	private MonomeCombo tapCombo;
	private MonomeCombo[] switchCombo;
	
	private ArrayList<MonomeSequencerBeatListener> beatListeners;
	
	
	public MonomeSequencer(OscP5 osc, String oscName, int listenPort, int nx, int ny)
	{
		super(osc, oscName, listenPort, nx, ny);
		
		patterns = new int[nx][nx][ny];
		patternIndex = 0;
		
		// Tap Tempo Combo
		tapCombo = new MonomeCombo();
		tapCombo.add(0, 7);
		tapCombo.add(7, 7);
		addCombo(tapCombo);
		
		// Pattern Switch Combos
		switchCombo = new MonomeCombo[nx];
		for(int i = 0; i < nx; i++)
		{
			switchCombo[i] = new MonomeCombo();
			switchCombo[i].add(0, 7);
			switchCombo[i].add(i, 0);
			addCombo(switchCombo[i]);
		}
		
		// Slice to Show Current Play Head Position
		playHeadSlice = new int[ny];
		for(int i = 0; i < ny; i++)
			playHeadSlice[i] = ON;
		
		beatListeners = new ArrayList<MonomeSequencerBeatListener>();
		
		metronome = new Metronome();
		metronome.bpm = 120;
		metronome.resolution = nx / 4;
		metronome.addListener(this);
		metronome.start();
		
		addListener(this);
	}
	
	
	public void addBeatListener(MonomeSequencerBeatListener listener)
	{
		beatListeners.add(listener);
	}
	
	public void setBpm(float bpm)
	{
		metronome.bpm = bpm;
	}
	
	
	public int[] getSlice(int pos)
	{
		int[][] pattern = patterns[patternIndex];
		int[] slice = new int[ny];
		for(int i = 0; i < ny; i++)
			slice[i] = pattern[i][pos];
		return slice;
	}
	
	public int[][] getPattern(int index)
	{
		return patterns[index];
	}

	
	public void onMonomeButton(MonomeButtonEvent event)
	{
		if(event.state == OFF)
		{
			int[][] pattern = patterns[patternIndex];
			int state = pattern[event.y][event.x] == OFF ? ON : OFF;
			pattern[event.y][event.x] = state;
			setLed(event.x, event.y, state);
		}
	}

	public void onMonomeCombo(MonomeComboEvent event)
	{
		if(event.combo == tapCombo)
		{
			metronome.tap();
			System.out.println(metronome.bpm);
		}
		else {
			for(int i = 0; i < switchCombo.length; i++)
			{
				if(event.combo == switchCombo[i])
				{
					patternIndex = i;
					setLedFrame(patterns[patternIndex]);
					break;
				}
			}
		}
	}


	public void onBeat(Metronome m)
	{
		setLedCol(position, getSlice(position));
		position = (position + 1) % nx;
		setLedCol(position, playHeadSlice);
		
		MonomeSequencerBeatEvent event = new MonomeSequencerBeatEvent(this);
		
		for(int i = 0, n = beatListeners.size(); i < n; i++)
			beatListeners.get(i).onMonomeSequenceBeat(event);
	}
	
	
	public class MonomeSequencerBeatEvent
	{
		public MonomeSequencer sequencer;
		
		public int[] slice;
		
		public MonomeSequencerBeatEvent(MonomeSequencer _sequencer)
		{
			sequencer = _sequencer;
			slice = getSlice(_sequencer.position);
		}
		
		public String toString()
		{
			String str = "[";
			for(int i = 0, n1 = slice.length - 1; i <= n1; i++)
			{
				str += String.valueOf(slice[i]);
				if(i < n1)
					str += ",";
			}
			return str + "]";
		}
	}
	
	public interface MonomeSequencerBeatListener
	{
		public void onMonomeSequenceBeat(MonomeSequencerBeatEvent event);
	}
}