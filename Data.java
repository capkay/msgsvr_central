// custom data object class definition
// to hold messages with addressing details(to & from) and timestamp, all as strings

public class Data
{
	public String from;
	public String to;
	public String time;
	public String msg;
	Data(String from,String to,String time,String msg)
	{
		this.from = from;
		this.to   = to;
		this.time = time;
		this.msg  = msg;
	}
}
