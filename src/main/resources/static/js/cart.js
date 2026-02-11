$(document).ready(function () {
    
    function formatNumber(num) {
        return Math.round(num).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }
    
    function updateCartUI(data, bookId) {
        if (!data.success) return;
        
        // Update item subtotal
        var $item = $(`.cart-item[data-id="${bookId}"]`);
        if ($item.length && data.itemSubtotal !== undefined) {
            $item.find('.cart-item-subtotal').text(formatNumber(data.itemSubtotal));
        }
        if ($item.length && data.itemQuantity !== undefined) {
            $item.find('.quantity-input').val(data.itemQuantity);
            // Disable decrease if qty = 1
            var $decBtn = $item.find('.quantity-btn[data-action="decrease"]');
            $decBtn.prop('disabled', data.itemQuantity <= 1);
        }
        
        // Update summary
        if (data.totalPrice !== undefined) {
            var priceStr = formatNumber(data.totalPrice);
            $('#summaryPrice').text(priceStr);
            $('#summaryTotal').text(priceStr);
        }
        if (data.totalQuantity !== undefined) {
            $('#summaryQuantity').text(data.totalQuantity);
            $('#cartItemCount').text(data.totalQuantity);
            
            // Update header cart badge
            var $badge = $('#cartBadge');
            if ($badge.length) {
                $badge.text(data.totalQuantity);
                if (data.totalQuantity > 0) {
                    $badge.show();
                } else {
                    $badge.hide();
                }
            }
        }
    }
    
    // Handle quantity increase/decrease buttons
    $('.quantity-btn').click(function () {
        var id = $(this).data('id');
        var action = $(this).data('action');
        var $input = $(`.quantity-input[data-id="${id}"]`);
        var quantity = parseInt($input.val());
        
        if (action === 'increase') {
            quantity++;
        } else if (action === 'decrease' && quantity > 1) {
            quantity--;
        } else {
            return;
        }
        
        // Optimistically update the input
        $input.val(quantity);
        
        // Update quantity via AJAX
        $.ajax({
            url: '/cart/updateCart/' + id + '/' + quantity,
            type: 'GET',
            dataType: 'json',
            success: function (data) {
                updateCartUI(data, id);
            },
            error: function () {
                // Fallback: reload on error
                location.reload();
            }
        });
    });
    
    // Handle direct quantity input change
    $('.quantity-input').change(function () {
        var quantity = parseInt($(this).val());
        var id = $(this).data('id');
        
        if (quantity < 1 || isNaN(quantity)) {
            quantity = 1;
            $(this).val(1);
        }
        
        $.ajax({
            url: '/cart/updateCart/' + id + '/' + quantity,
            type: 'GET',
            dataType: 'json',
            success: function (data) {
                updateCartUI(data, id);
            },
            error: function () {
                location.reload();
            }
        });
    });
});