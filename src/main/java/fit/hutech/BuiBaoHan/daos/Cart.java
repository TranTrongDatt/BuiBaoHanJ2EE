package fit.hutech.BuiBaoHan.daos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Data;

@Data
public class Cart {

    private List<Item> cartItems = new ArrayList<>();

    public void addItems(Item item) {
        boolean isExist = cartItems.stream()
                .filter(i -> Objects.equals(i.getBookId(),
                item.getBookId()))
                .findFirst()
                .map(i -> {
                    i.setQuantity(i.getQuantity()
                            + item.getQuantity());
                    return true;
                })
                .orElse(false);
        if (!isExist) {
            cartItems.add(item);
        }
    }

    public void removeItems(Long bookId) {
        cartItems.removeIf(item -> Objects.equals(item.getBookId(),
                bookId));
    }

    public void updateItems(Long bookId, int quantity) {
        cartItems.stream()
                .filter(item -> Objects.equals(item
                .getBookId(), bookId))
                .forEach(item -> item.setQuantity(quantity));
    }

    /**
     * Tính tổng số lượng sản phẩm trong giỏ hàng
     */
    public int getTotalQuantity() {
        return cartItems.stream().mapToInt(Item::getQuantity).sum();
    }
}
