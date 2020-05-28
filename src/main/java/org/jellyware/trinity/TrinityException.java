package org.jellyware.trinity;

import javax.ws.rs.core.Response;

import org.jellyware.beef.Beef.UncheckedException;

public class TrinityException extends UncheckedException {
    private static final long serialVersionUID = 7918826341720024323L;

    @Override
    public Response.Status httpStatus() {
        return Response.Status.UNAUTHORIZED;
    }

    public TrinityException(org.jellyware.beef.Error.Builder error, Throwable cause) {
        super(error, cause);
    }

    public TrinityException(org.jellyware.beef.Error.Builder error) {
        super(error);
    }

    public TrinityException(Throwable cause) {
        super(cause);
    }

    public TrinityException() {
    }
}