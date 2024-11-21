package model;

public class ResponseOrden {
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public ResponseOrden(Integer price) {
        this.price = price;
    }

    private Integer price;
}
