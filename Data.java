public class Data{
    String sdt="";
    Integer idt=0;
    boolean isInt=false;
    public String getData() {
        if(isInt)
            return ""+idt;
        return sdt;
    }
    public void setIntData(Integer dt) {
        idt = dt;
        isInt = true;
    }
    public void setStringData(String dt) {
        sdt = dt;
        isInt = false;
    }
}