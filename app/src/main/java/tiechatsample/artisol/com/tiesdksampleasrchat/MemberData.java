package tiechatsample.artisol.com.tiesdksampleasrchat;

class MemberData {
    private String name;
    private String color;

    public MemberData(String name, String color) {
        this.name = name;
        this.color = color;
    }

    // Add an empty constructor so we can later parse JSON into MemberData using Jackson
    public MemberData() {
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public void setName(String s){
        name=s;
    }

    public void setColor(String s){
        color=s;
    }
}
