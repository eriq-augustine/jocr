# Some base utiliities and eventual cross-platform utilities.

class AssertionError < RuntimeError
end

def assert(condition)
   if (!condition)
      raise AssertionError
   end
end
