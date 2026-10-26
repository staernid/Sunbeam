package io.github.staernid.sunbeam.computers;

import io.github.staernid.sunbeam.nvstream.http.ComputerDetails;

public interface ComputerManagerListener {
    void notifyComputerUpdated(ComputerDetails details);
}
