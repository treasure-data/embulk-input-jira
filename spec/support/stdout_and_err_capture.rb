module StdoutAndErrCapture
  def capture_output(output = :out, &block)
    java_import 'java.io.PrintStream'
    java_import 'java.io.ByteArrayOutputStream'
    java_import 'java.lang.System'

    ruby_original_stream = output == :out ? $stdout.dup : $stderr.dup
    java_original_stream = System.send(output) # :out or :err
    ruby_buf = StringIO.new
    java_buf = ByteArrayOutputStream.new

    case output
    when :out
      $stdout = ruby_buf
    when :err
      $stderr = ruby_buf
    end
    System.setOut(PrintStream.new(java_buf))

    block.call

    ruby_buf.string + java_buf.toString
  ensure
    System.setOut(java_original_stream)
    case output
    when :out
      $stdout = ruby_original_stream
    when :err
      $stderr = ruby_original_stream
    end
  end
end
