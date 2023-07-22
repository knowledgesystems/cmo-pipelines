/*
 * Copyright (c) 2023 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal CMO-Pipelines.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.cmo.pipelines.common.util;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.cbioportal.cmo.pipelines.common.util.PausingHttpHandler;

public class PausingHttpService {
    public static final int WEB_SERVER_UNAVAILABLE_PORT = -1;
    PausingHttpHandler pausingHttpHandler;
    HttpServer server;

    public PausingHttpService() {
        this.pausingHttpHandler = new PausingHttpHandler();
        this.server = null;
    }

    // users of this class can adjust pause duration and response content through the handler
    public PausingHttpHandler getHandler() {
        return pausingHttpHandler;
    }

    public boolean isAvailable() {
        return server != null;
    }

    public int getPort() {
        if (isAvailable()) {
            return server.getAddress().getPort();
        } else {
            return WEB_SERVER_UNAVAILABLE_PORT;
        }
    }

    public void start() {
        InetSocketAddress socket = new InetSocketAddress(0); // ephemeral port
        try {
            server = HttpServer.create(socket, 100); // backlog needed for testing dropped connections
            server.createContext("/", pausingHttpHandler);
            BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(128);
            server.setExecutor(new ThreadPoolExecutor(32, 128, 10, TimeUnit.SECONDS, workQueue));
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Could not start http service on any port\n", e);
        }
    }

    public void stop() {
        server.stop(1);
        server = null;
    }

}
