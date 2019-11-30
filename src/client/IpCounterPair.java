package client;

public class IpCounterPair implements Comparable<IpCounterPair>{

	private String ip;
	private int counter;
	
	public IpCounterPair(String ip) {
		this.ip = ip;
		counter = 0;
	}
	
	public String getIp() {
		return this.ip;
	}
	
	public int getCounter() {
		return this.counter;
	}
	
	public void incCounter() {
		this.counter++;
	}

	@Override
	public int compareTo(IpCounterPair other) {
		return this.getCounter() - other.getCounter();
	}
}
