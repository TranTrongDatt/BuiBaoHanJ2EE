package fit.hutech.BuiBaoHan.daos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    private Long bookId;
    private String bookName;
    private Double price;
    private int quantity;
    private String coverImage;
    private String slug;
    
    // Constructor without coverImage/slug for backward compatibility
    public Item(Long bookId, String bookName, Double price, int quantity) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.price = price;
        this.quantity = quantity;
    }
}
