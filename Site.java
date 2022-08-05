public class Site {

    public final int production;
    public int owner, strength;

    public Site(int production) {
        this.production = production;
    }

    @Override
    public String toString() {
        return "Site{" +
                "production=" + production +
                ", owner=" + owner +
                ", strength=" + strength +
                '}';
    }
}
