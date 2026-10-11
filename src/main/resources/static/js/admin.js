/**
 * SmartNav Live - Admin Dashboard, Moderation & Chart.js Analytics
 */

const AdminEngine = {
    charts: {},

    initDashboard: function () {
        this.loadAnalyticsCharts();
        this.bindModerationButtons();
    },

    loadAnalyticsCharts: function () {
        fetch('/api/admin/analytics')
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data) {
                    this.renderCategoryChart(res.data.categoryDistribution);
                    this.renderDailyTrendChart(res.data.dailyTrafficReports);
                    this.renderMonthlyGrowthChart(res.data.monthlyUserGrowth);
                }
            })
            .catch(err => console.error('Failed to load analytics', err));
    },

    renderCategoryChart: function (data) {
        const ctx = document.getElementById('categoryChart')?.getContext('2d');
        if (!ctx) return;

        const labels = Object.keys(data);
        const values = Object.values(data);

        const colors = [
            '#ef4444', // ACCIDENT
            '#f59e0b', // TRAFFIC_JAM
            '#dc2626', // ROAD_BLOCK
            '#ea580c', // CONSTRUCTION
            '#0284c7', // FLOOD
            '#3b82f6', // POLICE_CHECKPOINT
            '#8b5cf6'  // BROKEN_SIGNAL
        ];

        this.charts.category = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels.map(l => l.replace('_', ' ')),
                datasets: [{
                    data: values,
                    backgroundColor: colors,
                    borderWidth: 2,
                    borderColor: '#0f172a'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { color: '#94a3b8', font: { family: 'Plus Jakarta Sans' } } }
                },
                cutout: '70%'
            }
        });
    },

    renderDailyTrendChart: function (data) {
        const ctx = document.getElementById('dailyTrendChart')?.getContext('2d');
        if (!ctx) return;

        const labels = Object.keys(data);
        const values = Object.values(data);

        this.charts.daily = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Traffic Reports',
                    data: values,
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: '#10b981',
                    pointRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { ticks: { color: '#64748b' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                    y: { ticks: { color: '#64748b', stepSize: 1 }, grid: { color: 'rgba(255,255,255,0.05)' } }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    },

    renderMonthlyGrowthChart: function (data) {
        const ctx = document.getElementById('monthlyGrowthChart')?.getContext('2d');
        if (!ctx) return;

        const labels = Object.keys(data);
        const values = Object.values(data);

        this.charts.growth = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'New Users',
                    data: values,
                    backgroundColor: '#06b6d4',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { ticks: { color: '#64748b' }, grid: { display: false } },
                    y: { ticks: { color: '#64748b', stepSize: 1 }, grid: { color: 'rgba(255,255,255,0.05)' } }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    },

    bindModerationButtons: function () {
        // Approve Report
        document.querySelectorAll('.btn-approve-report').forEach(btn => {
            btn.addEventListener('click', e => {
                const reportId = e.currentTarget.dataset.id;
                fetch(`/api/admin/approve/${reportId}`, { method: 'PUT' })
                    .then(res => res.json())
                    .then(res => {
                        if (res.success) {
                            SmartNav.showToast({ title: 'Approved', message: 'Report is now published to live map.', type: 'success' });
                            setTimeout(() => location.reload(), 1000);
                        } else {
                            alert(res.message);
                        }
                    });
            });
        });

        // Reject Report
        document.querySelectorAll('.btn-reject-report').forEach(btn => {
            btn.addEventListener('click', e => {
                const reportId = e.currentTarget.dataset.id;
                const reason = prompt('Please specify a rejection reason:', 'Unverified incident');
                if (reason === null) return;

                fetch(`/api/admin/reject/${reportId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ reason: reason })
                })
                .then(res => res.json())
                .then(res => {
                    if (res.success) {
                        SmartNav.showToast({ title: 'Rejected', message: 'Report rejected.', type: 'warning' });
                        setTimeout(() => location.reload(), 1000);
                    }
                });
            });
        });

        // Delete Report
        document.querySelectorAll('.btn-delete-report').forEach(btn => {
            btn.addEventListener('click', e => {
                if (!confirm('Are you sure you want to permanently delete this report?')) return;
                const reportId = e.currentTarget.dataset.id;
                fetch(`/api/admin/reports/${reportId}`, { method: 'DELETE' })
                    .then(res => res.json())
                    .then(res => {
                        if (res.success) {
                            SmartNav.showToast({ title: 'Deleted', message: 'Report removed.', type: 'success' });
                            setTimeout(() => location.reload(), 1000);
                        }
                    });
            });
        });

        // Toggle User Status (with confirmation dialog)
        document.querySelectorAll('.btn-toggle-user').forEach(btn => {
            btn.addEventListener('click', e => {
                const userId = e.currentTarget.dataset.id;
                const currentStatus = e.currentTarget.dataset.status || 'ACTIVE';
                const userName = e.currentTarget.dataset.name || 'this user';
                const action = currentStatus === 'ACTIVE' ? 'suspend' : 'reactivate';

                if (!confirm(`Are you sure you want to ${action} ${userName} (#${userId})?`)) {
                    return;
                }

                fetch(`/api/admin/users/${userId}/toggle-status`, { method: 'PUT' })
                    .then(res => res.json())
                    .then(res => {
                        if (res.success) {
                            if (window.SmartNav && SmartNav.showToast) {
                                SmartNav.showToast({ title: 'User Updated', message: res.message, type: 'info' });
                            } else {
                                alert(res.message);
                            }
                            setTimeout(() => location.reload(), 800);
                        } else {
                            alert(res.message);
                        }
                    })
                    .catch(err => alert('Failed to update user status.'));
            });
        });

        // View User Details Modal Handler
        document.querySelectorAll('.btn-view-user').forEach(btn => {
            btn.addEventListener('click', e => {
                const userId = e.currentTarget.dataset.id;
                fetch(`/api/admin/users/${userId}`)
                    .then(res => res.json())
                    .then(res => {
                        if (res.success && res.data) {
                            const u = res.data;
                            document.getElementById('modalUserId').textContent = '#' + u.id;
                            document.getElementById('modalUserName').textContent = u.name;
                            document.getElementById('modalUserInitial').textContent = (u.name || 'U').substring(0, 1).toUpperCase();
                            document.getElementById('modalUserEmail').textContent = u.email;
                            document.getElementById('modalUserPhone').textContent = u.phone || 'Not provided';
                            
                            const roleEl = document.getElementById('modalUserRole');
                            roleEl.textContent = u.role;
                            roleEl.className = 'badge ' + (u.role === 'ADMIN' ? 'bg-danger' : 'bg-primary');

                            const statusEl = document.getElementById('modalUserStatus');
                            statusEl.textContent = u.status;
                            statusEl.className = 'badge ' + (u.status === 'ACTIVE' ? 'bg-success' : 'bg-danger');

                            document.getElementById('modalUserPoints').textContent = `${u.points || 0} pts (${u.badge || 'Bronze Scout'})`;
                            document.getElementById('modalUserReports').textContent = `${u.totalReports || 0} submitted (${u.approvedReports || 0} approved)`;
                            document.getElementById('modalUserCreated').textContent = u.createdAt ? u.createdAt.replace('T', ' ').substring(0, 16) : '—';
                            document.getElementById('modalUserLastLogin').textContent = u.lastLogin ? u.lastLogin.replace('T', ' ').substring(0, 16) : 'Never logged in';

                            const modalEl = document.getElementById('userDetailsModal');
                            if (modalEl && window.bootstrap) {
                                new bootstrap.Modal(modalEl).show();
                            }
                        } else {
                            alert('Could not retrieve user details.');
                        }
                    })
                    .catch(() => alert('Network error retrieving user details.'));
            });
        });

        // Export PDF
        document.getElementById('btnExportPdf')?.addEventListener('click', () => {
            window.print();
        });
    }
};

window.AdminEngine = AdminEngine;
