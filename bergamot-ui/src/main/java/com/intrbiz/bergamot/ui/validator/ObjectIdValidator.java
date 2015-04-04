package com.intrbiz.bergamot.ui.validator;

import static com.intrbiz.balsa.BalsaContext.*;

import java.lang.annotation.Annotation;
import java.util.UUID;

import com.intrbiz.bergamot.metadata.IsaObjectId;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.validator.ValidationException;
import com.intrbiz.validator.Validator;

public class ObjectIdValidator extends Validator<UUID>
{
    private boolean session = true;
    
    public ObjectIdValidator()
    {
        super(UUID.class);
    }

    @Override
    public void configure(Annotation data, Annotation[] additional)
    {
        if (data instanceof IsaObjectId)
        {
            this.session = ((IsaObjectId) data).session();
        }
    }

    @Override
    public UUID validate(UUID in) throws ValidationException
    {
        // must be not null
        if (in == null) throw new ValidationException("No object id given");
        // lookup the current site
        Site site =  this.session ? Balsa().sessionVar("site") : Balsa().var("site");
        // validate
        if (! site.isValidObjectId(in)) throw new ValidationException("The given object id is not valid for this site");
        return in;
    }
}
