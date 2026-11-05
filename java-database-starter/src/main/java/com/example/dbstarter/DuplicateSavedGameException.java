package com.example.dbstarter;

import java.sql.SQLException;

public final class DuplicateSavedGameException extends SQLException {
    private static final long serialVersionUID = 1L;

    public DuplicateSavedGameException(SQLException cause) {
        super("This game is already saved in the user's library.", cause);
    }
}
