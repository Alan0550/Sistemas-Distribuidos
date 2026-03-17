package edu.upb.tmservice;

import edu.upb.tmservice.dao.TicketDao;
import edu.upb.tmservice.dao.TicketReleaseItem;
import edu.upb.tmservice.dao.TipoTicketDao;
import edu.upb.tmservice.dao.UsuarioDao;
import edu.upb.tmservice.dao.UsuarioEntity;

import java.sql.Connection;
import java.util.List;

public class UserManagementService {
    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final TicketDao ticketDao = new TicketDao();
    private final TipoTicketDao tipoTicketDao = new TipoTicketDao();

    public BanResult banUser(long userId) throws Exception {
        return updateBanStatus(userId, true);
    }

    public BanResult unbanUser(long userId) throws Exception {
        return updateBanStatus(userId, false);
    }

    public String refreshAutomaticRole(Connection conn, long userId, String currentRole) throws Exception {
        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            return currentRole;
        }

        int totalTickets = ticketDao.countPurchasedTicketsByUser(conn, userId);
        String targetRole = targetRoleForPurchaseCount(totalTickets);
        if (roleRank(targetRole) > roleRank(currentRole)) {
            usuarioDao.updateRole(conn, userId, targetRole);
            return targetRole;
        }
        return currentRole;
    }

    private BanResult updateBanStatus(long userId, boolean banned) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                UsuarioEntity user = usuarioDao.findById(conn, userId);
                if (user == null) {
                    throw new IllegalArgumentException("Usuario no existe");
                }
                if ("ADMIN".equalsIgnoreCase(user.getRol())) {
                    throw new IllegalArgumentException("No se puede modificar a un usuario ADMIN");
                }

                int releasedTickets = 0;
                if (banned && !user.isBaneado()) {
                    List<TicketReleaseItem> releaseItems = ticketDao.loadFutureActiveTicketReleaseByUser(conn, userId);
                    for (TicketReleaseItem item : releaseItems) {
                        tipoTicketDao.increaseAvailable(conn, item.getTipoTicketId(), item.getQuantity());
                        releasedTickets += item.getQuantity();
                    }
                    ticketDao.cancelFutureActiveTicketsByUser(conn, userId);
                }

                usuarioDao.updateBaneado(conn, userId, banned);
                conn.commit();
                return new BanResult(user.getUsername(), banned, releasedTickets);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private String targetRoleForPurchaseCount(int totalTickets) {
        if (totalTickets >= 40) {
            return "VIP";
        }
        if (totalTickets >= 20) {
            return "FRECUENTE";
        }
        return "CLIENTE";
    }

    private int roleRank(String role) {
        if ("VIP".equalsIgnoreCase(role)) {
            return 3;
        }
        if ("FRECUENTE".equalsIgnoreCase(role)) {
            return 2;
        }
        if ("CLIENTE".equalsIgnoreCase(role)) {
            return 1;
        }
        return 0;
    }

    public static class BanResult {
        private final String username;
        private final boolean banned;
        private final int releasedTickets;

        public BanResult(String username, boolean banned, int releasedTickets) {
            this.username = username;
            this.banned = banned;
            this.releasedTickets = releasedTickets;
        }

        public String getUsername() {
            return username;
        }

        public boolean isBanned() {
            return banned;
        }

        public int getReleasedTickets() {
            return releasedTickets;
        }
    }
}
