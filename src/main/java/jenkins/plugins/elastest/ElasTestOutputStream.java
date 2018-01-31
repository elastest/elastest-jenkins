/*
 * The MIT License
 *
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.elastest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;

/**
 * Output stream that writes each line to the provided delegate output stream
 * and also sends it to ElasTest.
 *
 * @author Francisco R Díaz
 * @since 0.0.1
 */
public class ElasTestOutputStream extends LineTransformationOutputStream {
    final OutputStream delegate;
    final ElasTestWriter elasTestWriter;

    public ElasTestOutputStream(OutputStream delegate,
            ElasTestWriter elasTestWriter) {
        super();
        this.delegate = delegate;
        this.elasTestWriter = elasTestWriter;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        delegate.write(b, 0, len);
        this.flush();

        if (!elasTestWriter.isConnectionBroken()) {
            String line = new String(b, 0, len, StandardCharsets.UTF_8.name()).trim();
            line = ConsoleNote.removeNotes(line);
            elasTestWriter.write(line);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        delegate.flush();
        super.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        delegate.close();
        super.close();
    }
}
