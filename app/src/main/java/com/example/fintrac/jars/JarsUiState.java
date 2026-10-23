package com.example.fintrac.jars;

import com.example.fintrac.JarItem;
import com.example.fintrac.core.Money;

import java.util.Collections;
import java.util.List;

/** Everything the Jars screen needs to draw itself. Immutable. */
public final class JarsUiState {

    public final boolean loading;
    public final String error;
    public final List<JarItem> visibleJars;
    public final Money totalSaved;
    public final Money totalGoal;
    /** True when the user has jars but none match the current filter. */
    public final boolean filteredOut;

    JarsUiState(boolean loading, String error, List<JarItem> visibleJars,
                Money totalSaved, Money totalGoal, boolean filteredOut) {
        this.loading = loading;
        this.error = error;
        this.visibleJars = Collections.unmodifiableList(visibleJars);
        this.totalSaved = totalSaved;
        this.totalGoal = totalGoal;
        this.filteredOut = filteredOut;
    }

    static JarsUiState initial() {
        return new JarsUiState(true, null, Collections.emptyList(), Money.ZERO, Money.ZERO, false);
    }

    public boolean isEmpty() {
        return !loading && error == null && visibleJars.isEmpty();
    }
}
