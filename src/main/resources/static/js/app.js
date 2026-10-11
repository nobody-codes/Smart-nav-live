/**
 * SmartNav Live - Core Client Application & WebSocket Real-Time Telemetry
 */

const SmartNav = {
    stompClient: null,
    connected: false,
    listeners: {
        onReportUpdate: [],
        onNotification: []
    },

    init: function () {
        this.initWebSocket();
        this.loadUnreadNotifications();
        this.bindGlobalEvents();
    },

    initWebSocket: function () {
        const socket = new SockJS('/ws-traffic');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // Suppress noisy debug logs

        const self = this;
        this.stompClient.connect({}, function (frame) {
            self.connected = true;
            console.log('[SmartNav] WebSocket Connected: ' + frame);

            // Subscribe to live incident reports stream
            self.stompClient.subscribe('/topic/reports', function (message) {
                try {
                    const data = JSON.parse(message.body);
                    self.handleReportEvent(data);
                } catch (e) {
                    console.error('[SmartNav] Error parsing report message', e);
                }
            });

            // Subscribe to broadcast notifications
            self.stompClient.subscribe('/topic/notifications', function (message) {
                try {
                    const notification = JSON.parse(message.body);
                    self.handleNotificationEvent(notification);
                } catch (e) {
                    console.error('[SmartNav] Error parsing notification message', e);
                }
            });
        }, function (error) {
            console.warn('[SmartNav] WebSocket reconnecting in 5s...', error);
            setTimeout(() => self.initWebSocket(), 5000);
        });
    },

    handleReportEvent: function (event) {
        // Show floating real-time toast alert
        if (event.action === 'NEW_REPORT' || event.action === 'STATUS_CHANGE') {
            const report = event.report;
            if (report) {
                this.showToast({
                    title: `Traffic Alert: ${report.category}`,
                    message: `${report.address} - ${report.description.substring(0, 80)}...`,
                    type: report.category === 'ROAD_BLOCK' ? 'danger' : 'warning',
                    time: 'Just now'
                });
            }
        }

        // Notify registered subscribers (such as Leaflet map)
        this.listeners.onReportUpdate.forEach(cb => {
            try { cb(event); } catch (err) { console.error(err); }
        });
    },

    handleNotificationEvent: function (notification) {
        this.showToast({
            title: notification.title || 'New Notification',
            message: notification.message,
            type: 'info',
            time: 'Just now'
        });

        this.incrementNotificationBadge();

        this.listeners.onNotification.forEach(cb => {
            try { cb(notification); } catch (err) { console.error(err); }
        });
    },

    registerReportListener: function (callback) {
        this.listeners.onReportUpdate.push(callback);
    },

    showToast: function ({ title, message, type = 'info', time = 'Just now' }) {
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            document.body.appendChild(container);
        }

        const borderColors = {
            danger: '#ef4444',
            warning: '#f59e0b',
            success: '#10b981',
            info: '#06b6d4'
        };

        const iconClasses = {
            danger: 'fa-triangle-exclamation text-danger',
            warning: 'fa-car-burst text-warning',
            success: 'fa-circle-check text-success',
            info: 'fa-bell text-info'
        };

        const toast = document.createElement('div');
        toast.className = 'toast-smartnav';
        toast.style.borderColor = borderColors[type] || borderColors.info;

        toast.innerHTML = `
            <div class="d-flex justify-content-between align-items-center mb-1">
                <strong class="d-flex align-items-center gap-2">
                    <i class="fa-solid ${iconClasses[type] || iconClasses.info}"></i>
                    <span>${title}</span>
                </strong>
                <small class="text-secondary">${time}</small>
            </div>
            <div class="text-light small">${message}</div>
        `;

        container.appendChild(toast);

        // Auto remove after 6 seconds
        setTimeout(() => {
            toast.style.transition = 'all 0.4s ease';
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(15px)';
            setTimeout(() => toast.remove(), 400);
        }, 6000);
    },

    loadUnreadNotifications: function () {
        fetch('/api/notifications')
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data) {
                    const unread = res.data.filter(n => !n.isRead).length;
                    this.updateNotificationBadge(unread);
                }
            })
            .catch(() => {});
    },

    incrementNotificationBadge: function () {
        const badge = document.getElementById('navNotificationBadge');
        if (badge) {
            let current = parseInt(badge.textContent) || 0;
            current++;
            badge.textContent = current;
            badge.classList.remove('d-none');
        }
    },

    updateNotificationBadge: function (count) {
        const badge = document.getElementById('navNotificationBadge');
        if (badge) {
            if (count > 0) {
                badge.textContent = count;
                badge.classList.remove('d-none');
            } else {
                badge.classList.add('d-none');
            }
        }
    },

    bindGlobalEvents: function () {
        // Quick demo login helper
        window.fillDemoCredentials = function (role) {
            const emailInput = document.getElementById('email');
            const passInput = document.getElementById('password');
            if (!emailInput || !passInput) return;

            if (role === 'admin') {
                emailInput.value = 'admin@smartnav.com';
                passInput.value = 'admin123';
            } else if (role === 'user') {
                emailInput.value = 'john.doe@smartnav.com';
                passInput.value = 'user123';
            } else if (role === 'guest') {
                emailInput.value = 'guest@smartnav.com';
                passInput.value = 'guest123';
            }
        };
    }
};

document.addEventListener('DOMContentLoaded', () => SmartNav.init());
