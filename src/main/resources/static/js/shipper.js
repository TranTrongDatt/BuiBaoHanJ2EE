/**
 * Shipper Dashboard JavaScript
 * MiniVerse E-commerce Platform
 */

// CSRF Token helper
function getCsrfToken() {
    const token = document.querySelector('meta[name="_csrf"]');
    return token ? token.content : '';
}

function getCsrfHeader() {
    const header = document.querySelector('meta[name="_csrf_header"]');
    return header ? header.content : 'X-CSRF-TOKEN';
}

// Toggle Sidebar (Mobile)
function toggleShipperSidebar() {
    const sidebar = document.getElementById('shipperSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    if (sidebar) {
        sidebar.classList.toggle('active');
    }
    if (overlay) {
        overlay.classList.toggle('active');
    }
}

// Update Shipper Status
function updateStatus(status) {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    fetch('/shipper/orders/status?status=' + status, {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Update UI
            document.querySelectorAll('.status-btn').forEach(btn => {
                btn.classList.remove('active');
                btn.classList.remove('btn-success', 'btn-secondary');
                btn.classList.add('btn-outline-success', 'btn-outline-secondary');
            });
            
            // Find and activate the clicked button
            const activeBtn = event.target.closest('.status-btn');
            if (activeBtn) {
                activeBtn.classList.add('active');
                if (status === 'ONLINE') {
                    activeBtn.classList.remove('btn-outline-success');
                    activeBtn.classList.add('btn-success');
                } else {
                    activeBtn.classList.remove('btn-outline-secondary');
                    activeBtn.classList.add('btn-secondary');
                }
            }
            
            // Update status badge in topbar
            const badge = document.querySelector('.shipper-status-badge');
            if (badge) {
                badge.className = 'shipper-status-badge ' + status;
                badge.querySelector('span').textContent = getStatusText(status);
            }
            
            showToast('success', 'Đã cập nhật trạng thái!');
        } else {
            showToast('error', data.message || 'Có lỗi xảy ra');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Không thể cập nhật trạng thái');
    });
}

function getStatusText(status) {
    const texts = {
        'ONLINE': 'Online',
        'OFFLINE': 'Offline',
        'BUSY': 'Đang giao',
        'ON_BREAK': 'Đang nghỉ'
    };
    return texts[status] || status;
}

// Accept Order
function acceptOrder(orderId) {
    if (!confirm('Xác nhận nhận đơn hàng này?')) return;
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    fetch('/shipper/orders/' + orderId + '/accept', {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Đã nhận đơn hàng thành công!');
            setTimeout(() => location.reload(), 1000);
        } else {
            showToast('error', data.message || 'Không thể nhận đơn hàng');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Có lỗi xảy ra');
    });
}

// Start Delivery
function startDelivery(orderId) {
    if (!confirm('Xác nhận bắt đầu giao hàng?')) return;
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    fetch('/shipper/orders/' + orderId + '/start-delivery', {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Đã bắt đầu giao hàng!');
            setTimeout(() => location.reload(), 1000);
        } else {
            showToast('error', data.message || 'Không thể bắt đầu giao hàng');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Có lỗi xảy ra');
    });
}

// Complete Delivery
function completeDelivery(orderId) {
    // Show modal or form for notes and proof image
    const modal = new bootstrap.Modal(document.getElementById('completeDeliveryModal'));
    document.getElementById('completeOrderId').value = orderId;
    modal.show();
}

function submitCompleteDelivery() {
    const orderId = document.getElementById('completeOrderId').value;
    const notes = document.getElementById('deliveryNotes').value;
    const proofImage = document.getElementById('proofImage').files[0];
    
    const formData = new FormData();
    if (notes) formData.append('notes', notes);
    if (proofImage) formData.append('proofImage', proofImage);
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    fetch('/shipper/orders/' + orderId + '/complete', {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Giao hàng thành công!');
            setTimeout(() => location.href = '/shipper/orders', 1000);
        } else {
            showToast('error', data.message || 'Không thể hoàn thành giao hàng');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Có lỗi xảy ra');
    });
}

// Do Attendance Punch
function doPunch(punchType) {
    // Get location if available
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                sendPunch(punchType, position.coords.latitude, position.coords.longitude);
            },
            () => {
                sendPunch(punchType, null, null);
            }
        );
    } else {
        sendPunch(punchType, null, null);
    }
}

function sendPunch(punchType, lat, lng) {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    let url = '/shipper/attendance/punch?punchType=' + punchType;
    if (lat != null && lng != null) {
        url += '&latitude=' + lat + '&longitude=' + lng;
    }
    
    fetch(url, {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', data.message);
            setTimeout(() => location.reload(), 1500);
        } else {
            showToast('error', data.message || 'Chấm công thất bại');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Có lỗi xảy ra khi chấm công');
    });
}

// Format number with commas
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Format currency
function formatCurrency(amount) {
    return formatNumber(Math.round(amount)) + ' VNĐ';
}

// Toast Notification
function showToast(type, message) {
    // Remove existing toasts
    document.querySelectorAll('.toast-notification').forEach(el => el.remove());
    
    const toast = document.createElement('div');
    toast.className = 'toast-notification toast-' + type;
    toast.innerHTML = `
        <i class="bi ${type === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'}"></i>
        <span>${message}</span>
    `;
    
    // Add styles
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 25px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        gap: 10px;
        font-weight: 500;
        z-index: 9999;
        animation: slideIn 0.3s ease;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    
    if (type === 'success') {
        toast.style.background = '#d4edda';
        toast.style.color = '#155724';
    } else {
        toast.style.background = '#f8d7da';
        toast.style.color = '#721c24';
    }
    
    document.body.appendChild(toast);
    
    // Auto remove after 3 seconds
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Add animation styles
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', function() {
    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', function(e) {
        const sidebar = document.getElementById('shipperSidebar');
        const toggle = document.querySelector('.topbar-toggle');
        
        if (sidebar && sidebar.classList.contains('active')) {
            if (!sidebar.contains(e.target) && e.target !== toggle && !toggle.contains(e.target)) {
                toggleShipperSidebar();
            }
        }
    });
    
    // Auto-refresh dashboard every 5 minutes
    if (window.location.pathname === '/shipper' || window.location.pathname === '/shipper/dashboard') {
        setInterval(() => {
            location.reload();
        }, 5 * 60 * 1000);
    }
});
